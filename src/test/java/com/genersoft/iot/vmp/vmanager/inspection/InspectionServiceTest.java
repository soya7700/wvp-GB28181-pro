package com.genersoft.iot.vmp.vmanager.inspection;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiRule;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.ChannelHealth;
import com.genersoft.iot.vmp.vmanager.inspection.bean.ModelQuality;
import com.genersoft.iot.vmp.vmanager.inspection.bean.SceneTemplate;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AlgorithmDefinition;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AlgorithmEvent;
import com.genersoft.iot.vmp.vmanager.inspection.bean.MaintenanceWindow;
import com.genersoft.iot.vmp.vmanager.inspection.bean.SceneRiskSummary;
import com.genersoft.iot.vmp.vmanager.inspection.bean.MobileRecorder;
import com.genersoft.iot.vmp.vmanager.inspection.bean.RecorderLocation;
import com.genersoft.iot.vmp.vmanager.inspection.bean.StoreVisitTask;
import com.genersoft.iot.vmp.vmanager.inspection.bean.VisitChecklistResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.VisitMediaFile;
import com.genersoft.iot.vmp.vmanager.inspection.dao.InspectionMapper;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionService;
import com.genersoft.iot.vmp.vmanager.inspection.service.AiInspectionClient;
import com.genersoft.iot.vmp.vmanager.inspection.conf.InspectionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    @Mock
    private InspectionMapper mapper;
    @Mock
    private IDeviceAlarmService alarmService;
    @Mock
    private AiInspectionClient aiClient;
    @Mock
    private InspectionProperties properties;
    private InspectionService service;

    @BeforeEach
    void setUp() {
        service = new InspectionService();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "alarmService", alarmService);
        ReflectionTestUtils.setField(service, "aiClient", aiClient);
        ReflectionTestUtils.setField(service, "properties", properties);
    }

    @Test
    void createPlanShouldFillDefaults() {
        InspectionPlan plan = new InspectionPlan();
        plan.setName("夜间质量巡检");
        plan.setIntervalMinutes(15);

        service.create(plan);

        assertEquals("BLACK_SCREEN,FREEZE,BLUR,OCCLUSION", plan.getDetectionTypes());
        assertNotNull(plan.getCreateTime());
        verify(mapper).insertPlan(plan);
    }

    @Test
    void createPlanShouldRejectInvalidInterval() {
        InspectionPlan plan = new InspectionPlan();
        plan.setName("错误计划");
        plan.setIntervalMinutes(0);

        assertThrows(ControllerException.class, () -> service.create(plan));
        verifyNoInteractions(mapper);
    }

    @Test
    void confirmedReviewShouldCreateAlarmAndPersistReview() {
        InspectionResult result = pendingResult();
        when(mapper.result(10L)).thenReturn(result);
        when(mapper.review(any(InspectionResult.class))).thenReturn(1);

        InspectionResult reviewed = service.review(10L, "CONFIRMED", "现场确认", 7);

        assertEquals("CONFIRMED", reviewed.getStatus());
        assertEquals(Integer.valueOf(7), reviewed.getReviewedBy());
        ArgumentCaptor<DeviceAlarm> alarmCaptor = ArgumentCaptor.forClass(DeviceAlarm.class);
        verify(alarmService).add(alarmCaptor.capture());
        assertNotNull(alarmCaptor.getValue().getCreateTime());
        assertEquals(alarmCaptor.getValue().getAlarmTime(), alarmCaptor.getValue().getCreateTime());
        verify(mapper).review(result);
        verify(mapper).insertWorkOrder(argThat(order ->
                Long.valueOf(10L).equals(order.getResultId()) && "OPEN".equals(order.getStatus())));
    }

    @Test
    void falsePositiveShouldNotCreateAlarm() {
        InspectionResult result = pendingResult();
        when(mapper.result(10L)).thenReturn(result);
        when(mapper.review(any(InspectionResult.class))).thenReturn(1);

        service.review(10L, "FALSE_POSITIVE", null, 7);

        verifyNoInteractions(alarmService);
        assertEquals("FALSE_POSITIVE", result.getStatus());
    }

    @Test
    void reviewShouldRejectAlreadyReviewedResult() {
        InspectionResult result = pendingResult();
        result.setStatus("CONFIRMED");
        when(mapper.result(10L)).thenReturn(result);

        assertThrows(ControllerException.class, () -> service.review(10L, "CONFIRMED", null, 7));
        verify(mapper, never()).review(any());
        verifyNoInteractions(alarmService);
    }

    @Test
    void createRuleShouldValidateThreshold() {
        AiRule rule = new AiRule();
        rule.setName("区域入侵");
        rule.setDetectionType("INTRUSION");
        rule.setConfidenceThreshold(1.1);

        assertThrows(ControllerException.class, () -> service.createRule(rule));
        verifyNoInteractions(mapper);
    }

    @Test
    void duplicateCallbackShouldReturnExistingResult() {
        InspectionResult callback = pendingResult();
        callback.setCallbackId("callback-1");
        InspectionResult existing = pendingResult();
        existing.setId(22L);
        when(mapper.resultByCallbackId("callback-1")).thenReturn(existing);

        InspectionResult result = service.addResult(9L, callback);

        assertSame(existing, result);
        verify(mapper, never()).insertResult(any());
    }

    @Test
    void callbackIdShouldBeRequired() {
        InspectionResult callback = pendingResult();

        assertThrows(ControllerException.class, () -> service.addResult(9L, callback));
        verify(mapper, never()).insertResult(any());
    }

    @Test
    void scheduleShouldRespectDayAndTimeWindow() {
        InspectionPlan plan = new InspectionPlan();
        plan.setScheduleDays("1,3,5");
        plan.setStartTime("08:00");
        plan.setEndTime("18:00");

        assertTrue(service.isWithinSchedule(plan, LocalDateTime.of(2026, 7, 24, 10, 0)));
        assertFalse(service.isWithinSchedule(plan, LocalDateTime.of(2026, 7, 23, 10, 0)));
        assertFalse(service.isWithinSchedule(plan, LocalDateTime.of(2026, 7, 24, 20, 0)));
    }

    @Test
    void invalidScheduleWindowShouldBeRejected() {
        InspectionPlan plan = new InspectionPlan();
        plan.setName("夜间计划");
        plan.setIntervalMinutes(30);
        plan.setStartTime("18:00");
        plan.setEndTime("08:00");

        assertThrows(ControllerException.class, () -> service.create(plan));
        verify(mapper, never()).insertPlan(any());
    }

    @Test
    void recentDuplicateShouldBeMerged() {
        InspectionResult callback = pendingResult();
        callback.setCallbackId("callback-2");
        callback.setConfidence(0.97);
        InspectionResult existing = pendingResult();
        existing.setId(30L);
        existing.setOccurrenceCount(2);
        when(mapper.recentOpenResult(eq("channel-1"), eq("BLACK_SCREEN"), anyString()))
                .thenReturn(existing);

        InspectionResult result = service.addResult(9L, callback);

        assertEquals(Integer.valueOf(3), result.getOccurrenceCount());
        verify(mapper).mergeResult(callback);
        verify(mapper, never()).insertResult(any());
    }

    @Test
    void incidentsWithSameDeviceAndRootCauseShouldAggregate() {
        InspectionResult callback = pendingResult();
        callback.setCallbackId("callback-group");
        callback.setDeviceId("device-a");
        InspectionResult existing = pendingResult();
        existing.setId(40L);
        existing.setOccurrenceCount(4);
        when(mapper.recentIncident(eq("device-a:CAMERA_OR_SCENE"), anyString())).thenReturn(existing);

        InspectionResult result = service.addResult(11L, callback);

        assertEquals(Integer.valueOf(5), result.getOccurrenceCount());
        assertEquals("CAMERA_OR_SCENE", callback.getRootCause());
        verify(mapper).mergeResult(callback);
        verify(mapper, never()).insertResult(any());
    }

    @Test
    void emptyIncidentKeyShouldNotRecoverBroadly() {
        assertThrows(ControllerException.class, () -> service.recoverIncident(" "));
        verify(mapper, never()).recoverIncident(anyString(), anyString());
    }

    @Test
    void modelQualityShouldFlagHighFalsePositiveRate() {
        ModelQuality quality = new ModelQuality();
        quality.setTotalCount(100);
        quality.setConfirmedCount(50);
        quality.setFalsePositiveCount(40);
        when(mapper.modelQuality()).thenReturn(Collections.singletonList(quality));

        ModelQuality result = service.modelQuality().get(0);

        assertEquals(0.5, result.getConfirmationRate(), 0.001);
        assertEquals(0.4, result.getFalsePositiveRate(), 0.001);
        assertEquals("DRIFT_RISK", result.getQualityStatus());
    }

    @Test
    void invalidModelRolloutShouldBeRejected() {
        assertThrows(ControllerException.class, () -> service.rolloutModel(1, 101));
        verify(mapper, never()).rolloutModel(anyInt(), anyInt());
    }

    @Test
    void foodServicePresetShouldUseGenericSceneRegions() {
        doAnswer(invocation -> {
            SceneTemplate template = invocation.getArgument(0);
            template.setId(12);
            return 1;
        }).when(mapper).insertSceneTemplate(any(SceneTemplate.class));
        SceneTemplate stored = new SceneTemplate();
        stored.setId(12);
        when(mapper.sceneTemplate(12)).thenReturn(stored);

        SceneTemplate result = service.createFoodServicePreset();

        assertEquals("FOOD_SERVICE", result.getCode());
        verify(mapper, times(3)).insertSceneRegion(argThat(region ->
                Integer.valueOf(12).equals(region.getTemplateId())
                        && region.getAlgorithmCodes() != null));
    }

    @Test
    void sceneRegionShouldRequireExistingTemplate() {
        assertThrows(ControllerException.class,
                () -> service.createSceneRegion(99, new com.genersoft.iot.vmp.vmanager.inspection.bean.SceneRegion()));
        verify(mapper, never()).insertSceneRegion(any());
    }

    @Test
    void personnelPresetShouldCreateSixGenericAlgorithms() {
        when(mapper.insertAlgorithm(any(AlgorithmDefinition.class))).thenReturn(1);

        assertEquals(6, service.createPersonnelAlgorithms());
        verify(mapper, times(6)).insertAlgorithm(argThat(definition ->
                "PERSONNEL".equals(definition.getCategory()) && Boolean.TRUE.equals(definition.getEnabled())));
    }

    @Test
    void shortAlgorithmEventShouldRemainObserving() {
        AlgorithmDefinition definition = algorithmDefinition();
        when(mapper.algorithm("NO_MASK")).thenReturn(definition);
        AlgorithmEvent event = algorithmEvent(1, 0.95);

        assertEquals("OBSERVING", service.receiveAlgorithmEvent(event).getState());
        verify(mapper).insertAlgorithmEvent(event);
    }

    @Test
    void qualifyingAlgorithmEventShouldOpenAndDeduplicate() {
        AlgorithmDefinition definition = algorithmDefinition();
        when(mapper.algorithm("NO_MASK")).thenReturn(definition);
        AlgorithmEvent event = algorithmEvent(5, 0.95);

        assertEquals("OPEN", service.receiveAlgorithmEvent(event).getState());
        assertEquals("channel-1:NO_MASK:person-1", event.getDedupKey());

        AlgorithmEvent existing = new AlgorithmEvent();
        existing.setId(8L);
        when(mapper.openAlgorithmEvent(event.getDedupKey())).thenReturn(existing);
        assertSame(existing, service.receiveAlgorithmEvent(event));
        verify(mapper, times(1)).insertAlgorithmEvent(any());
    }

    @Test
    void environmentPresetShouldCreateEightAlgorithms() {
        when(mapper.insertAlgorithm(any(AlgorithmDefinition.class))).thenReturn(1);
        assertEquals(8, service.createEnvironmentAlgorithms());
        verify(mapper, times(8)).insertAlgorithm(argThat(item -> "ENVIRONMENT".equals(item.getCategory())));
    }

    @Test
    void maintenanceWindowShouldSuppressMatchingEvent() {
        when(mapper.algorithm("NO_MASK")).thenReturn(algorithmDefinition());
        MaintenanceWindow window = new MaintenanceWindow();
        window.setEndTime("2026-07-24 12:00:00");
        when(mapper.activeMaintenanceWindow(eq("channel-1"), isNull(), anyString())).thenReturn(window);
        AlgorithmEvent event = service.receiveAlgorithmEvent(algorithmEvent(5, 0.95));
        assertEquals("SUPPRESSED", event.getState());
        assertEquals(window.getEndTime(), event.getSuppressedUntil());
    }

    @Test
    void maintenanceWindowShouldValidateTimeRange() {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setScopeType("CHANNEL");
        window.setScopeId("channel-1");
        window.setStartTime("2026-07-24 12:00:00");
        window.setEndTime("2026-07-24 10:00:00");
        assertThrows(ControllerException.class, () -> service.createMaintenanceWindow(window));
    }

    @Test
    void eventReviewShouldRejectUnsupportedStatus() {
        assertThrows(ControllerException.class, () -> service.reviewAlgorithmEvent(1L, "IGNORED", null, 7));
        verify(mapper, never()).reviewAlgorithmEvent(anyLong(), anyString(), anyInt(), any(), anyString());
    }

    @Test
    void sceneRiskShouldClassifyWeightedScore() {
        SceneRiskSummary summary = new SceneRiskSummary();
        summary.setRiskScore(31);
        when(mapper.sceneRiskSummaries()).thenReturn(Collections.singletonList(summary));
        assertEquals("CRITICAL", service.sceneRiskSummaries().get(0).getRiskLevel());
    }

    @Test
    void recorderCapabilitiesShouldRespectProtocolLimits() {
        MobileRecorder recorder = new MobileRecorder();
        recorder.setDeviceCode("REC-001");
        recorder.setName("巡店记录仪");
        recorder.setProtocolType("GB28181");
        service.createMobileRecorder(recorder);
        assertTrue(recorder.getCapabilities().contains("LIVE_VIDEO"));
        assertFalse(recorder.getCapabilities().contains("SNAPSHOT"));
        assertEquals("AVAILABLE", recorder.getStatus());
        verify(mapper).insertMobileRecorder(recorder);
    }

    @Test
    void duplicateRecorderCodeShouldBeRejected() {
        MobileRecorder recorder = new MobileRecorder();
        recorder.setDeviceCode("REC-001");
        recorder.setName("巡店记录仪");
        recorder.setProtocolType("VENDOR_SDK");
        when(mapper.mobileRecorderByCode("REC-001")).thenReturn(new MobileRecorder());
        assertThrows(ControllerException.class, () -> service.createMobileRecorder(recorder));
    }

    @Test
    void invalidRecorderLocationShouldBeRejected() {
        when(mapper.mobileRecorder(1L)).thenReturn(new MobileRecorder());
        RecorderLocation location = new RecorderLocation();
        location.setLongitude(181D);
        location.setLatitude(30D);
        assertThrows(ControllerException.class, () -> service.recordLocation(1L, location));
        verify(mapper, never()).insertRecorderLocation(any());
    }

    @Test
    void distantStoreCheckinShouldBeRejected() {
        StoreVisitTask task = new StoreVisitTask();
        task.setStoreLongitude(120.1);
        task.setStoreLatitude(30.2);
        when(mapper.storeVisitTask(1L)).thenReturn(task);
        assertThrows(ControllerException.class, () -> service.checkinStoreVisitTask(1L, 121.5, 31.2));
        verify(mapper, never()).checkinStoreVisitTask(anyLong(), anyDouble(), anyString());
    }

    @Test
    void checklistSubmissionShouldBeIdempotent() {
        StoreVisitTask task = new StoreVisitTask();
        task.setStatus("IN_PROGRESS");
        when(mapper.storeVisitTask(1L)).thenReturn(task);
        VisitChecklistResult input = new VisitChecklistResult();
        input.setSubmissionId("submit-1");
        input.setItemCode("HYGIENE-1");
        input.setResult("PASS");
        VisitChecklistResult existing = new VisitChecklistResult();
        existing.setId(9L);
        when(mapper.checklistResultBySubmission("submit-1")).thenReturn(existing);
        assertSame(existing, service.submitChecklistResult(1L, input, 7));
        verify(mapper, never()).insertChecklistResult(any());
    }

    @Test
    void requiredChecklistEvidenceShouldBeEnforced() {
        StoreVisitTask task = new StoreVisitTask();
        task.setStatus("IN_PROGRESS");
        when(mapper.storeVisitTask(1L)).thenReturn(task);
        VisitChecklistResult input = new VisitChecklistResult();
        input.setSubmissionId("submit-2");
        input.setItemCode("HYGIENE-2");
        input.setResult("FAIL");
        input.setEvidenceRequired(true);
        assertThrows(ControllerException.class, () -> service.submitChecklistResult(1L, input, 7));
    }

    @Test
    void unsupportedRecorderCommandShouldBeRejected() {
        MobileRecorder recorder = new MobileRecorder();
        recorder.setCapabilities("LIVE_VIDEO,LOCATION");
        when(mapper.mobileRecorder(1L)).thenReturn(recorder);
        assertThrows(ControllerException.class, () -> service.requireRecorderCapability(1L, "SNAPSHOT"));
    }

    @Test
    void streamQuotaShouldReturnClearFailure() {
        MobileRecorder recorder = new MobileRecorder();
        recorder.setCapabilities("LIVE_VIDEO");
        when(mapper.mobileRecorder(1L)).thenReturn(recorder);
        when(mapper.activeStreamLeaseCount(eq("tenant-1"), anyString())).thenReturn(8);
        assertThrows(ControllerException.class,
                () -> service.acquireStreamLease("tenant-1", 1L, "MOBILE_PREVIEW", 8));
        verify(mapper, never()).insertStreamLease(any());
    }

    @Test
    void mediaRegistrationShouldBeIdempotent() {
        when(mapper.storeVisitTask(1L)).thenReturn(new StoreVisitTask());
        VisitMediaFile input = new VisitMediaFile();
        input.setUploadId("upload-1");
        input.setMediaType("VIDEO");
        input.setFileName("visit.mp4");
        input.setFileSize(1024L);
        VisitMediaFile existing = new VisitMediaFile();
        existing.setId(5L);
        when(mapper.visitMediaByUploadId("upload-1")).thenReturn(existing);
        assertSame(existing, service.registerVisitMedia(1L, input, 7));
        verify(mapper, never()).insertVisitMedia(any());
    }

    @Test
    void claimShouldRejectConcurrentClaim() {
        when(mapper.claimResult(10L, 7)).thenReturn(0);

        assertThrows(ControllerException.class, () -> service.claim(10L, 7));
    }

    @Test
    void healthShouldReportMigrationAndAiState() {
        when(mapper.schemaTableCount()).thenReturn(12);
        when(aiClient.configured()).thenReturn(true);
        when(properties.getServiceUrl()).thenReturn("http://ai-service");

        assertTrue(service.health().isMigrationReady());
        assertTrue(service.health().isAiConfigured());
        assertEquals("READY", service.health().getStatus());
    }

    private AlgorithmDefinition algorithmDefinition() {
        AlgorithmDefinition definition = new AlgorithmDefinition();
        definition.setCode("NO_MASK");
        definition.setMinDurationSeconds(3);
        definition.setConfidenceThreshold(0.82);
        return definition;
    }

    private AlgorithmEvent algorithmEvent(int duration, double confidence) {
        AlgorithmEvent event = new AlgorithmEvent();
        event.setEventUid("event-" + duration);
        event.setAlgorithmCode("NO_MASK");
        event.setChannelId("channel-1");
        event.setTargetId("person-1");
        event.setDurationSeconds(duration);
        event.setConfidence(confidence);
        return event;
    }

    @Test
    void dispatchShouldRetryAndRecover() {
        InspectionPlan plan = new InspectionPlan();
        plan.setId(3);
        plan.setChannelIds("channel-1");
        when(mapper.plan(3)).thenReturn(plan);
        when(aiClient.configured()).thenReturn(true);
        when(properties.getMaxRetries()).thenReturn(2);
        when(properties.getRetryDelayMillis()).thenReturn(0L);
        doThrow(new RuntimeException("temporary"))
                .doThrow(new RuntimeException("temporary"))
                .doNothing()
                .when(aiClient).dispatch(eq(plan), any());

        assertEquals("WAITING_AI", service.run(3).getStatus());
        verify(aiClient, times(3)).dispatch(eq(plan), any());
        verify(mapper, atLeastOnce()).updateTaskStatus(argThat(
                task -> "WAITING_AI".equals(task.getStatus())
                        && Integer.valueOf(2).equals(task.getRetryCount())));
    }

    @Test
    void scheduleIntervalShouldBeCalculatedInJava() {
        InspectionPlan plan = new InspectionPlan();
        plan.setId(8);
        plan.setIntervalMinutes(30);
        when(mapper.latestTaskStart(8)).thenReturn("2026-07-23 10:00:00");

        assertFalse(service.isDue(plan, LocalDateTime.of(2026, 7, 23, 10, 29)));
        assertTrue(service.isDue(plan, LocalDateTime.of(2026, 7, 23, 10, 30)));
    }

    @Test
    void cleanupShouldRemoveMessagesAndAlarmsBeforeInspectionRows() {
        service.cleanupTestData();

        org.mockito.InOrder order = inOrder(mapper);
        order.verify(mapper).deleteTestMessages();
        order.verify(mapper).deleteTestAlarms();
        order.verify(mapper).deleteTestWorkOrders();
        order.verify(mapper).deleteTestChannelHealth();
        order.verify(mapper).deleteTestResults();
        order.verify(mapper).deleteTestTasks();
        order.verify(mapper).deleteTestRules();
        order.verify(mapper).deleteTestModels();
        order.verify(mapper).deleteTestPlans();
    }

    @Test
    void healthScoreShouldExposeStreamFailureEvenWhenDeviceOnline() {
        ChannelHealth health = new ChannelHealth();
        health.setChannelId("channel-1");
        health.setOnline(true);
        health.setStreamAvailable(false);
        health.setRecordingComplete(true);
        health.setVideoQualityScore(100);

        service.recordHealth(health);

        assertEquals(Integer.valueOf(75), health.getHealthScore());
        assertEquals("WARNING", health.getHealthStatus());
        verify(mapper).insertChannelHealth(health);
    }

    @Test
    void offlineChannelShouldBeCritical() {
        ChannelHealth health = new ChannelHealth();
        health.setChannelId("channel-2");
        health.setOnline(false);
        health.setStreamAvailable(false);
        health.setRecordingComplete(false);
        health.setVideoQualityScore(40);

        service.recordHealth(health);

        assertEquals(Integer.valueOf(0), health.getHealthScore());
        assertEquals("CRITICAL", health.getHealthStatus());
    }

    @Test
    void workOrderCanOnlyBeResolvedByAssignee() {
        when(mapper.resolveWorkOrder(eq(3L), eq(7), eq("更换摄像机"), anyString())).thenReturn(0);

        assertThrows(ControllerException.class,
                () -> service.resolveWorkOrder(3L, 7, "更换摄像机"));
    }

    @Test
    void workOrderResolutionMustNotBeEmpty() {
        assertThrows(ControllerException.class, () -> service.resolveWorkOrder(3L, 7, " "));
        verify(mapper, never()).resolveWorkOrder(anyLong(), anyInt(), anyString(), anyString());
    }

    private InspectionResult pendingResult() {
        InspectionResult result = new InspectionResult();
        result.setId(10L);
        result.setDeviceId("device-1");
        result.setChannelId("channel-1");
        result.setDetectionType("BLACK_SCREEN");
        result.setStatus("PENDING");
        return result;
    }
}
