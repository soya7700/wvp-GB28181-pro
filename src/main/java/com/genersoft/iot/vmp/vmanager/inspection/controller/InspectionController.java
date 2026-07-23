package com.genersoft.iot.vmp.vmanager.inspection.controller;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionOverview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Tag(name = "AI巡检")
@RestController
@RequestMapping("/api/ai/inspection")
public class InspectionController {

    @Value("${ai.inspection.enabled:false}")
    private boolean enabled;

    @Value("${ai.inspection.service-url:}")
    private String serviceUrl;

    @GetMapping("/overview")
    @Operation(summary = "查询AI巡检接入状态与能力", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public InspectionOverview overview() {
        InspectionOverview result = new InspectionOverview();
        result.setEnabled(enabled);
        result.setServiceStatus(enabled && !serviceUrl.isEmpty() ? "READY" : "NOT_CONFIGURED");
        result.setServiceUrl(serviceUrl);
        result.setPhase("FOUNDATION");
        result.setCapabilities(Arrays.asList(
                capability("SNAPSHOT", "通道截图", "AVAILABLE", "复用现有流媒体截图能力"),
                capability("ALARM", "告警闭环", "AVAILABLE", "支持确认、处理、误报和站内信"),
                capability("BLACK_SCREEN", "黑屏检测", enabled ? "PLANNED" : "WAITING", "等待AI服务接入"),
                capability("FREEZE", "画面冻结", enabled ? "PLANNED" : "WAITING", "等待AI服务接入"),
                capability("BLUR", "清晰度检测", enabled ? "PLANNED" : "WAITING", "等待AI服务接入"),
                capability("OCCLUSION", "画面遮挡", enabled ? "PLANNED" : "WAITING", "等待AI服务接入")
        ));
        return result;
    }

    private InspectionOverview.Capability capability(String code, String name, String status, String description) {
        return new InspectionOverview.Capability(code, name, status, description);
    }
}
