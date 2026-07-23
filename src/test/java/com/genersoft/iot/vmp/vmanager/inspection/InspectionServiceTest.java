package com.genersoft.iot.vmp.vmanager.inspection;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiRule;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
import com.genersoft.iot.vmp.vmanager.inspection.dao.InspectionMapper;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionService;
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

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    @Mock
    private InspectionMapper mapper;
    @Mock
    private IDeviceAlarmService alarmService;
    private InspectionService service;

    @BeforeEach
    void setUp() {
        service = new InspectionService();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "alarmService", alarmService);
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
