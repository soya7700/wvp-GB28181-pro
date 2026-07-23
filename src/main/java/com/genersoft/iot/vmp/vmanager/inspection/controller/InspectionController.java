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
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionAnalytics;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionHealth;
import com.genersoft.iot.vmp.vmanager.inspection.bean.ChannelHealth;
import com.genersoft.iot.vmp.vmanager.inspection.bean.HealthDashboard;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionWorkOrder;
import com.genersoft.iot.vmp.vmanager.inspection.bean.IncidentGroup;
import com.genersoft.iot.vmp.vmanager.inspection.bean.ModelQuality;
import com.genersoft.iot.vmp.vmanager.inspection.bean.SceneTemplate;
import com.genersoft.iot.vmp.vmanager.inspection.bean.SceneRegion;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AlgorithmDefinition;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AlgorithmEvent;
import com.genersoft.iot.vmp.vmanager.inspection.bean.MaintenanceWindow;
import com.genersoft.iot.vmp.vmanager.inspection.bean.SceneRiskSummary;
import com.genersoft.iot.vmp.vmanager.inspection.bean.MobileRecorder;
import com.genersoft.iot.vmp.vmanager.inspection.bean.RecorderLocation;
import com.genersoft.iot.vmp.vmanager.inspection.bean.StoreVisitTask;
import com.genersoft.iot.vmp.vmanager.inspection.bean.VisitChecklistResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.VisitMediaFile;
import com.genersoft.iot.vmp.vmanager.inspection.bean.StreamLease;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionService;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionCallbackVerifier;
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

    @Autowired
    private InspectionCallbackVerifier callbackVerifier;

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

    @PutMapping("/plans/{id}")
    public InspectionPlan updatePlan(@PathVariable Integer id, @RequestBody InspectionPlan plan) {
        return service.update(id, plan);
    }

    @PostMapping("/plans/{id}/copy")
    public InspectionPlan copyPlan(@PathVariable Integer id) {
        return service.copy(id);
    }

    @DeleteMapping("/plans/{id}")
    public void deletePlan(@PathVariable Integer id) {
        service.delete(id);
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
    public InspectionResult receiveResult(@PathVariable Long id, @RequestBody InspectionResult result,
                                          @RequestHeader(value = "X-AI-Timestamp", required = false) String timestamp,
                                          @RequestHeader(value = "X-AI-Nonce", required = false) String nonce,
                                          @RequestHeader(value = "X-AI-Signature", required = false) String signature) {
        callbackVerifier.verify(timestamp, nonce, signature, id, "result", result.getCallbackId());
        return service.addResult(id, result);
    }

    @PostMapping("/tasks/{id}/complete")
    public void complete(@PathVariable Long id, @RequestBody InspectionTask completion,
                         @RequestHeader(value = "X-AI-Timestamp", required = false) String timestamp,
                         @RequestHeader(value = "X-AI-Nonce", required = false) String nonce,
                         @RequestHeader(value = "X-AI-Signature", required = false) String signature) {
        callbackVerifier.verify(timestamp, nonce, signature, id, "complete", "");
        service.complete(id, completion);
    }

    @PostMapping("/results/{id}/review")
    public InspectionResult review(@PathVariable Long id, @RequestParam String status,
                                   @RequestParam(required = false) String note) {
        return service.review(id, status, note, SecurityUtils.getUserId());
    }

    @PostMapping("/results/{id}/claim")
    public InspectionResult claim(@PathVariable Long id) {
        return service.claim(id, SecurityUtils.getUserId());
    }

    @PostMapping("/results/{id}/assign")
    public InspectionResult assign(@PathVariable Long id, @RequestParam Integer userId) {
        return service.assign(id, userId);
    }

    @PostMapping("/results/{id}/handle")
    public InspectionResult handle(@PathVariable Long id, @RequestParam String status,
                                   @RequestParam(required = false) String note) {
        return service.handle(id, status, note, SecurityUtils.getUserId());
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

    @PutMapping("/models/{id}/rollout")
    public void rolloutModel(@PathVariable Integer id, @RequestParam Integer percent) {
        service.rolloutModel(id, percent);
    }

    @GetMapping("/models/quality")
    public List<ModelQuality> modelQuality() {
        return service.modelQuality();
    }

    @GetMapping("/rules")
    public List<AiRule> rules() { return service.rules(); }

    @PostMapping("/rules")
    public AiRule createRule(@RequestBody AiRule rule) { return service.createRule(rule); }

    @GetMapping("/effects")
    public List<DetectionEffect> effects() { return service.effects(); }

    @GetMapping("/analytics")
    public InspectionAnalytics analytics(@RequestParam(defaultValue = "7") int days) {
        return service.analytics(days);
    }

    @GetMapping("/health")
    public InspectionHealth health() {
        return service.health();
    }

    @GetMapping("/channel-health")
    public HealthDashboard channelHealth() {
        return service.healthDashboard();
    }

    @PostMapping("/channel-health")
    public ChannelHealth recordChannelHealth(@RequestBody ChannelHealth health) {
        return service.recordHealth(health);
    }

    @GetMapping("/work-orders")
    public List<InspectionWorkOrder> workOrders() {
        return service.workOrders();
    }

    @PostMapping("/work-orders/{id}/accept")
    public InspectionWorkOrder acceptWorkOrder(@PathVariable Long id) {
        return service.acceptWorkOrder(id, SecurityUtils.getUserId());
    }

    @PostMapping("/work-orders/{id}/resolve")
    public InspectionWorkOrder resolveWorkOrder(@PathVariable Long id, @RequestParam String resolution) {
        return service.resolveWorkOrder(id, SecurityUtils.getUserId(), resolution);
    }

    @PostMapping("/work-orders/{id}/verify")
    public InspectionWorkOrder verifyWorkOrder(@PathVariable Long id, @RequestParam boolean passed) {
        return service.verifyWorkOrder(id, passed);
    }

    @GetMapping("/incidents")
    public List<IncidentGroup> incidents() {
        return service.incidentGroups();
    }

    @PostMapping("/incidents/recover")
    public int recoverIncident(@RequestParam String aggregationKey) {
        return service.recoverIncident(aggregationKey);
    }

    @GetMapping("/scene-templates")
    public List<SceneTemplate> sceneTemplates() { return service.sceneTemplates(); }

    @PostMapping("/scene-templates")
    public SceneTemplate createSceneTemplate(@RequestBody SceneTemplate template) {
        return service.createSceneTemplate(template);
    }

    @PostMapping("/scene-templates/presets/food-service")
    public SceneTemplate createFoodServicePreset() { return service.createFoodServicePreset(); }

    @GetMapping("/scene-templates/{id}/regions")
    public List<SceneRegion> sceneRegions(@PathVariable Integer id) { return service.sceneRegions(id); }

    @PostMapping("/scene-templates/{id}/regions")
    public SceneRegion createSceneRegion(@PathVariable Integer id, @RequestBody SceneRegion region) {
        return service.createSceneRegion(id, region);
    }

    @GetMapping("/algorithms")
    public List<AlgorithmDefinition> algorithms() { return service.algorithms(); }

    @PostMapping("/algorithms/presets/personnel")
    public int createPersonnelAlgorithms() { return service.createPersonnelAlgorithms(); }

    @PostMapping("/algorithms/presets/environment")
    public int createEnvironmentAlgorithms() { return service.createEnvironmentAlgorithms(); }

    @GetMapping("/algorithm-events")
    public List<AlgorithmEvent> algorithmEvents() { return service.algorithmEvents(); }

    @PostMapping("/algorithm-events")
    public AlgorithmEvent receiveAlgorithmEvent(@RequestBody AlgorithmEvent event) {
        return service.receiveAlgorithmEvent(event);
    }

    @PostMapping("/algorithm-events/{id}/recover")
    public int recoverAlgorithmEvent(@PathVariable Long id) { return service.recoverAlgorithmEvent(id); }

    @GetMapping("/maintenance-windows")
    public List<MaintenanceWindow> maintenanceWindows() { return service.maintenanceWindows(); }

    @PostMapping("/maintenance-windows")
    public MaintenanceWindow createMaintenanceWindow(@RequestBody MaintenanceWindow window) {
        return service.createMaintenanceWindow(window);
    }

    @PostMapping("/algorithm-events/{id}/review")
    public AlgorithmEvent reviewAlgorithmEvent(@PathVariable Long id, @RequestParam String status,
                                               @RequestParam(required = false) String note) {
        return service.reviewAlgorithmEvent(id, status, note, SecurityUtils.getUserInfo().getId());
    }

    @GetMapping("/scene-risk")
    public List<SceneRiskSummary> sceneRisk() { return service.sceneRiskSummaries(); }

    @GetMapping("/mobile-recorders")
    public List<MobileRecorder> mobileRecorders() { return service.mobileRecorders(); }

    @PostMapping("/mobile-recorders")
    public MobileRecorder createMobileRecorder(@RequestBody MobileRecorder recorder) {
        return service.createMobileRecorder(recorder);
    }

    @PostMapping("/mobile-recorders/{id}/assign")
    public MobileRecorder assignMobileRecorder(@PathVariable Long id,
                                               @RequestParam(required = false) Integer userId) {
        return service.assignMobileRecorder(id, userId);
    }

    @PostMapping("/mobile-recorders/{id}/locations")
    public RecorderLocation recordLocation(@PathVariable Long id, @RequestBody RecorderLocation location) {
        return service.recordLocation(id, location);
    }

    @GetMapping("/mobile-recorders/{id}/locations")
    public List<RecorderLocation> recorderLocations(@PathVariable Long id) {
        return service.recorderLocations(id);
    }

    @GetMapping("/store-visits")
    public List<StoreVisitTask> storeVisits() { return service.storeVisitTasks(); }

    @PostMapping("/store-visits")
    public StoreVisitTask createStoreVisit(@RequestBody StoreVisitTask task) {
        return service.createStoreVisitTask(task);
    }

    @PostMapping("/store-visits/{id}/checkin")
    public StoreVisitTask checkin(@PathVariable Long id, @RequestParam double longitude,
                                  @RequestParam double latitude) {
        return service.checkinStoreVisitTask(id, longitude, latitude);
    }

    @PostMapping("/store-visits/{id}/checkout")
    public StoreVisitTask checkout(@PathVariable Long id) { return service.checkoutStoreVisitTask(id); }

    @PostMapping("/store-visits/{id}/check-results")
    public VisitChecklistResult submitCheckResult(@PathVariable Long id,
                                                  @RequestBody VisitChecklistResult result) {
        return service.submitChecklistResult(id, result, SecurityUtils.getUserId());
    }

    @GetMapping("/store-visits/{id}/check-results")
    public List<VisitChecklistResult> checkResults(@PathVariable Long id) {
        return service.checklistResults(id);
    }

    @PostMapping("/mobile-recorders/{id}/stream-leases")
    public StreamLease acquireStreamLease(@PathVariable Long id, @RequestParam String tenantId,
                                          @RequestParam String businessType,
                                          @RequestParam(defaultValue = "8") int quota) {
        return service.acquireStreamLease(tenantId, id, businessType, quota);
    }

    @DeleteMapping("/stream-leases/{token}")
    public int releaseStreamLease(@PathVariable String token) { return service.releaseStreamLease(token); }

    @PostMapping("/store-visits/{id}/media")
    public VisitMediaFile registerMedia(@PathVariable Long id, @RequestBody VisitMediaFile media) {
        return service.registerVisitMedia(id, media, SecurityUtils.getUserId());
    }

    @PutMapping("/store-visits/media/{id}/progress")
    public VisitMediaFile updateMediaProgress(@PathVariable Long id, @RequestBody VisitMediaFile media) {
        return service.updateVisitMediaProgress(id, media);
    }

    @GetMapping("/store-visits/{id}/media")
    public List<VisitMediaFile> visitMedia(@PathVariable Long id) { return service.visitMedia(id); }

    @DeleteMapping("/test-data")
    public int cleanupTestData() {
        if (SecurityUtils.getUserInfo().getRole().getId() != 1) {
            throw new com.genersoft.iot.vmp.conf.exception.ControllerException(
                    com.genersoft.iot.vmp.vmanager.bean.ErrorCode.ERROR403);
        }
        return service.cleanupTestData();
    }

    private InspectionOverview.Capability capability(String code, String name, String status, String description) {
        return new InspectionOverview.Capability(code, name, status, description);
    }

}
