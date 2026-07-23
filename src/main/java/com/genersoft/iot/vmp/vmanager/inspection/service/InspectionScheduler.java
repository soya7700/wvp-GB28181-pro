package com.genersoft.iot.vmp.vmanager.inspection.service;

import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.conf.InspectionProperties;
import com.genersoft.iot.vmp.vmanager.inspection.dao.InspectionMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class InspectionScheduler {
    private final InspectionMapper mapper;
    private final InspectionService service;
    private final InspectionProperties properties;

    public InspectionScheduler(InspectionMapper mapper, InspectionService service,
                               InspectionProperties properties) {
        this.mapper = mapper;
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${ai.inspection.scheduler-delay-ms:60000}")
    public void schedule() {
        if (!properties.isEnabled()) return;
        for (InspectionPlan plan : mapper.duePlans()) {
            if (service.isWithinSchedule(plan, LocalDateTime.now())) {
                service.run(plan.getId());
            }
        }
    }
}
