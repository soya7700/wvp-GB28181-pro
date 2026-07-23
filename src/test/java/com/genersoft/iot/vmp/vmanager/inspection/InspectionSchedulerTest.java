package com.genersoft.iot.vmp.vmanager.inspection;

import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.conf.InspectionProperties;
import com.genersoft.iot.vmp.vmanager.inspection.dao.InspectionMapper;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionDistributedLock;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionScheduler;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InspectionSchedulerTest {

    @Test
    void schedulerShouldSkipPlanWhenAnotherInstanceOwnsLock() {
        InspectionMapper mapper = mock(InspectionMapper.class);
        InspectionService service = mock(InspectionService.class);
        InspectionProperties properties = mock(InspectionProperties.class);
        InspectionDistributedLock lock = mock(InspectionDistributedLock.class);
        InspectionPlan plan = new InspectionPlan();
        plan.setId(5);
        when(properties.isEnabled()).thenReturn(true);
        when(mapper.duePlans()).thenReturn(Collections.singletonList(plan));
        when(service.isWithinSchedule(eq(plan), any(LocalDateTime.class))).thenReturn(true);
        when(lock.acquire(anyString(), anyLong())).thenReturn(null);

        new InspectionScheduler(mapper, service, properties, lock).schedule();

        verify(service, never()).run(anyInt());
    }

    @Test
    void schedulerShouldReleaseLockAfterRun() {
        InspectionMapper mapper = mock(InspectionMapper.class);
        InspectionService service = mock(InspectionService.class);
        InspectionProperties properties = mock(InspectionProperties.class);
        InspectionDistributedLock lock = mock(InspectionDistributedLock.class);
        InspectionPlan plan = new InspectionPlan();
        plan.setId(6);
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getSchedulerLockSeconds()).thenReturn(120L);
        when(mapper.duePlans()).thenReturn(Collections.singletonList(plan));
        when(service.isWithinSchedule(eq(plan), any(LocalDateTime.class))).thenReturn(true);
        when(lock.acquire(anyString(), eq(120L))).thenReturn("token");

        new InspectionScheduler(mapper, service, properties, lock).schedule();

        verify(service).run(6);
        verify(lock).release("WVP:AI:INSPECTION:SCHEDULE:6", "token");
    }
}
