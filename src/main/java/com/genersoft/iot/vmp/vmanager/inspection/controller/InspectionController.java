package com.genersoft.iot.vmp.vmanager.inspection.controller;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionOverview;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionTask;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionReport;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiModel;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiRule;
import com.genersoft.iot.vmp.vmanager.inspection.bean.DetectionEffect;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "AI巡检")
@RestController
@RequestMapping("/api/ai/inspection")
public class InspectionController {

    @Value("${ai.inspection.enabled:false}")
    private boolean enabled;

    @Value("${ai.inspection.service-url:}")
    private String serviceUrl;

    @Autowired
    private InspectionService service;

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

    @GetMapping("/plans")
    public PageInfo<InspectionPlan> plans(@RequestParam int page, @RequestParam int count) {
        return service.plans(page, count);
    }

    @PostMapping("/plans")
    public InspectionPlan createPlan(@RequestBody InspectionPlan plan) {
        return service.create(plan);
    }

    @PutMapping("/plans/{id}/enabled")
    public InspectionPlan togglePlan(@PathVariable Integer id, @RequestParam boolean enabled) {
        return service.toggle(id, enabled);
    }

    @PostMapping("/plans/{id}/run")
    public InspectionTask run(@PathVariable Integer id) {
        return service.run(id);
    }

    @GetMapping("/tasks")
    public PageInfo<InspectionTask> tasks(@RequestParam int page, @RequestParam int count) {
        return service.tasks(page, count);
    }

    @GetMapping("/results")
    public PageInfo<InspectionResult> results(@RequestParam int page, @RequestParam int count,
                                               @RequestParam(required = false) String status) {
        return service.results(page, count, status);
    }

    @PostMapping("/tasks/{id}/results")
    public InspectionResult receiveResult(@PathVariable Long id, @RequestBody InspectionResult result) {
        return service.addResult(id, result);
    }

    @PostMapping("/tasks/{id}/complete")
    public void complete(@PathVariable Long id, @RequestBody InspectionTask completion) {
        service.complete(id, completion);
    }

    @PostMapping("/results/{id}/review")
    public InspectionResult review(@PathVariable Long id, @RequestParam String status,
                                   @RequestParam(required = false) String note) {
        return service.review(id, status, note, SecurityUtils.getUserId());
    }

    @GetMapping("/report")
    public InspectionReport report(@RequestParam String day) {
        return service.report(day);
    }

    @GetMapping("/models")
    public List<AiModel> models() { return service.models(); }

    @PostMapping("/models")
    public AiModel createModel(@RequestBody AiModel model) { return service.createModel(model); }

    @PutMapping("/models/{id}/activate")
    public void activateModel(@PathVariable Integer id) { service.activateModel(id); }

    @GetMapping("/rules")
    public List<AiRule> rules() { return service.rules(); }

    @PostMapping("/rules")
    public AiRule createRule(@RequestBody AiRule rule) { return service.createRule(rule); }

    @GetMapping("/effects")
    public List<DetectionEffect> effects() { return service.effects(); }

    private InspectionOverview.Capability capability(String code, String name, String status, String description) {
        return new InspectionOverview.Capability(code, name, status, description);
    }
}
