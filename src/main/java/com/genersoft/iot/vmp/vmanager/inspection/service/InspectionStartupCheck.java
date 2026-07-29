package com.genersoft.iot.vmp.vmanager.inspection.service;

import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionHealth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InspectionStartupCheck implements ApplicationRunner {
    private final InspectionService service;

    public InspectionStartupCheck(InspectionService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        InspectionHealth health = service.health();
        if (!health.isMigrationReady()) {
            log.warn("[AI巡检] 数据库迁移未完成，当前表数量：{}，请执行2.7.5巡检迁移脚本", health.getTableCount());
        } else {
            log.info("[AI巡检] 数据库结构检查通过，AI服务状态：{}", health.getStatus());
        }
    }
}
