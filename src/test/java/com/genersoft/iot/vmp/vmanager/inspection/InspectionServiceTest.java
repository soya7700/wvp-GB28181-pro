package com.genersoft.iot.vmp.vmanager.inspection;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiRule;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
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
    void claimShouldRejectConcurrentClaim() {
        when(mapper.claimResult(10L, 7)).thenReturn(0);

        assertThrows(ControllerException.class, () -> service.claim(10L, 7));
    }

    @Test
    void healthShouldReportMigrationAndAiState() {
        when(mapper.schemaTableCount()).thenReturn(5);
        when(aiClient.configured()).thenReturn(true);
        when(properties.getServiceUrl()).thenReturn("http://ai-service");

        assertTrue(service.health().isMigrationReady());
        assertTrue(service.health().isAiConfigured());
        assertEquals("READY", service.health().getStatus());
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
