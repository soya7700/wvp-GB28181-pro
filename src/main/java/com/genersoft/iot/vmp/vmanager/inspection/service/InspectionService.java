package com.genersoft.iot.vmp.vmanager.inspection.service;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
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
import com.genersoft.iot.vmp.vmanager.inspection.bean.VisitRectification;
import com.genersoft.iot.vmp.vmanager.inspection.bean.VisitOperationsSummary;
import java.util.UUID;
import java.util.Arrays;
import com.genersoft.iot.vmp.vmanager.inspection.conf.InspectionProperties;
import com.genersoft.iot.vmp.vmanager.inspection.dao.InspectionMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class InspectionService {
    @Autowired
    private InspectionMapper mapper;

    @Autowired
    private IDeviceAlarmService alarmService;

    @Autowired
    private AiInspectionClient aiClient;

    @Autowired
    private InspectionProperties properties;

    public PageInfo<InspectionPlan> plans(int page, int count) {
        PageHelper.startPage(page, count);
        return new PageInfo<>(mapper.plans());
    }

    public PageInfo<InspectionTask> tasks(int page, int count) {
        PageHelper.startPage(page, count);
        return new PageInfo<>(mapper.tasks());
    }

    public PageInfo<InspectionResult> results(int page, int count, String status) {
        PageHelper.startPage(page, count);
        return new PageInfo<>(mapper.results(status));
    }

    public InspectionPlan create(InspectionPlan plan) {
        validatePlan(plan);
        plan.setCreateTime(DateUtil.getNow());
        plan.setUpdateTime(plan.getCreateTime());
        mapper.insertPlan(plan);
        return plan;
    }

    public InspectionPlan update(Integer id, InspectionPlan changes) {
        InspectionPlan plan = requiredPlan(id);
        changes.setId(id);
        changes.setCreateTime(plan.getCreateTime());
        validatePlan(changes);
        changes.setUpdateTime(DateUtil.getNow());
        mapper.updatePlan(changes);
        return changes;
    }

    public InspectionPlan copy(Integer id) {
        InspectionPlan source = requiredPlan(id);
        source.setId(null);
        source.setName(source.getName() + " 副本");
        return create(source);
    }

    public void delete(Integer id) {
        requiredPlan(id);
        mapper.deletePlan(id);
    }

    public InspectionPlan toggle(Integer id, boolean enabled) {
        InspectionPlan plan = requiredPlan(id);
        plan.setEnabled(enabled);
        plan.setUpdateTime(DateUtil.getNow());
        mapper.togglePlan(plan);
        return plan;
    }

    public InspectionTask run(Integer planId) {
        InspectionPlan plan = requiredPlan(planId);
        InspectionTask task = new InspectionTask();
        task.setPlanId(planId);
        task.setStatus(aiClient.configured() ? "DISPATCHING" : "WAITING_AI");
        task.setChannelTotal(countChannels(plan.getChannelIds()));
        task.setStartTime(DateUtil.getNow());
        mapper.insertTask(task);
        if (aiClient.configured()) {
            RuntimeException lastError = null;
            int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    aiClient.dispatch(plan, task);
                    task.setRetryCount(attempt - 1);
                    updateTaskStatus(task, "WAITING_AI", null);
                    lastError = null;
                    break;
                } catch (RuntimeException error) {
                    lastError = error;
                    task.setRetryCount(attempt);
                    if (attempt < maxAttempts && properties.getRetryDelayMillis() > 0) {
                        try {
                            Thread.sleep(properties.getRetryDelayMillis());
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            if (lastError != null) {
                updateTaskStatus(task, "FAILED", "AI服务派发失败：" + lastError.getMessage());
            }
        }
        return task;
    }

    @Transactional
    public InspectionResult addResult(Long taskId, InspectionResult result) {
        if (result.getCallbackId() == null || result.getCallbackId().trim().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "callbackId不能为空");
        }
        InspectionResult existing = mapper.resultByCallbackId(result.getCallbackId());
        if (existing != null) return existing;
        String since = LocalDateTime.now().minusMinutes(5)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        result.setRootCause(inferRootCause(result.getDetectionType()));
        result.setAggregationKey((result.getDeviceId() == null ? result.getChannelId() : result.getDeviceId())
                + ":" + result.getRootCause());
        InspectionResult grouped = mapper.recentIncident(result.getAggregationKey(), since);
        if (grouped != null) {
            result.setId(grouped.getId());
            mapper.mergeResult(result);
            grouped.setOccurrenceCount((grouped.getOccurrenceCount() == null ? 1 : grouped.getOccurrenceCount()) + 1);
            return grouped;
        }
        InspectionResult duplicate = mapper.recentOpenResult(
                result.getChannelId(), result.getDetectionType(), since);
        if (duplicate != null) {
            result.setId(duplicate.getId());
            mapper.mergeResult(result);
            duplicate.setOccurrenceCount((duplicate.getOccurrenceCount() == null ? 1 : duplicate.getOccurrenceCount()) + 1);
            return duplicate;
        }
        result.setTaskId(taskId);
        result.setStatus("PENDING");
        result.setWorkflowStatus("NEW");
        if (result.getPriority() == null) result.setPriority(priority(result.getConfidence()));
        result.setCreateTime(DateUtil.getNow());
        mapper.insertResult(result);
        return result;
    }

    public InspectionResult claim(Long id, Integer userId) {
        if (mapper.claimResult(id, userId) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "异常已被认领或已关闭");
        }
        return mapper.result(id);
    }

    public InspectionResult assign(Long id, Integer userId) {
        if (mapper.assignResult(id, userId) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "异常不存在或已关闭");
        }
        return mapper.result(id);
    }

    public InspectionResult handle(Long id, String workflowStatus, String note, Integer userId) {
        if (!"PROCESSING".equals(workflowStatus) && !"CLOSED".equals(workflowStatus)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "处置状态仅支持 PROCESSING 或 CLOSED");
        }
        InspectionResult result = mapper.result(id);
        if (result == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检结果不存在");
        result.setAssigneeId(userId);
        result.setWorkflowStatus(workflowStatus);
        result.setHandlingNote(note);
        result.setHandledAt(DateUtil.getNow());
        if (mapper.handleResult(result) == 0) {
            throw new ControllerException(ErrorCode.ERROR403.getCode(), "仅负责人可以处置该异常");
        }
        return mapper.result(id);
    }

    public void complete(Long taskId, InspectionTask completion) {
        completion.setId(taskId);
        completion.setStatus(completion.getErrorMessage() == null ? "COMPLETED" : "FAILED");
        completion.setEndTime(DateUtil.getNow());
        mapper.completeTask(completion);
    }

    @Transactional
    public InspectionResult review(Long id, String status, String note, Integer userId) {
        if (!"CONFIRMED".equals(status) && !"FALSE_POSITIVE".equals(status)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "复核状态仅支持 CONFIRMED 或 FALSE_POSITIVE");
        }
        InspectionResult result = mapper.result(id);
        if (result == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检结果不存在");
        if (!"PENDING".equals(result.getStatus())) throw new ControllerException(ErrorCode.ERROR400.getCode(), "该异常已经复核");
        result.setStatus(status);
        result.setReviewNote(note);
        result.setReviewedBy(userId);
        result.setReviewedAt(DateUtil.getNow());
        if ("CONFIRMED".equals(status)) {
            String alarmTime = DateUtil.getNow();
            DeviceAlarm alarm = new DeviceAlarm();
            alarm.setDeviceId(result.getDeviceId());
            alarm.setChannelId(result.getChannelId());
            alarm.setAlarmPriority("2");
            alarm.setAlarmMethod("5");
            alarm.setAlarmType(result.getDetectionType());
            alarm.setAlarmTime(alarmTime);
            alarm.setAlarmDescription("AI巡检异常：" + detectionName(result.getDetectionType()));
            alarm.setCreateTime(alarmTime);
            alarmService.add(alarm);
            if (alarm.getId() != null) result.setAlarmId(Integer.parseInt(alarm.getId()));
        }
        if (mapper.review(result) == 0) throw new ControllerException(ErrorCode.ERROR400.getCode(), "该异常已经被其他用户复核");
        if ("CONFIRMED".equals(status)) createWorkOrder(result);
        return result;
    }

    private void createWorkOrder(InspectionResult result) {
        InspectionWorkOrder order = new InspectionWorkOrder();
        order.setResultId(result.getId());
        order.setTitle("AI巡检异常：" + detectionName(result.getDetectionType()));
        order.setPriority(result.getPriority() == null ? "NORMAL" : result.getPriority());
        order.setStatus("OPEN");
        int hours = "URGENT".equals(order.getPriority()) ? 2 : "HIGH".equals(order.getPriority()) ? 8 : 24;
        order.setDueTime(LocalDateTime.now().plusHours(hours)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        order.setCreateTime(DateUtil.getNow());
        order.setUpdateTime(order.getCreateTime());
        mapper.insertWorkOrder(order);
    }

    public List<InspectionWorkOrder> workOrders() {
        return mapper.workOrders();
    }

    public InspectionWorkOrder acceptWorkOrder(Long id, Integer userId) {
        String now = DateUtil.getNow();
        if (mapper.acceptWorkOrder(id, userId, now) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "工单已被接单或不存在");
        }
        return mapper.workOrder(id);
    }

    public InspectionWorkOrder resolveWorkOrder(Long id, Integer userId, String resolution) {
        if (resolution == null || resolution.trim().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "处理结果不能为空");
        }
        String now = DateUtil.getNow();
        if (mapper.resolveWorkOrder(id, userId, resolution, now) == 0) {
            throw new ControllerException(ErrorCode.ERROR403.getCode(), "仅接单人可以提交解决结果");
        }
        return mapper.workOrder(id);
    }

    public InspectionWorkOrder verifyWorkOrder(Long id, boolean passed) {
        String status = passed ? "CLOSED" : "OPEN";
        if (mapper.verifyWorkOrder(id, status, DateUtil.getNow()) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "仅待复核工单可以复核");
        }
        return mapper.workOrder(id);
    }

    public List<IncidentGroup> incidentGroups() {
        return mapper.incidentGroups();
    }

    public int recoverIncident(String aggregationKey) {
        if (aggregationKey == null || aggregationKey.trim().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "聚合事件编号不能为空");
        }
        return mapper.recoverIncident(aggregationKey, DateUtil.getNow());
    }

    String inferRootCause(String detectionType) {
        if (detectionType == null) return "UNKNOWN";
        if (detectionType.contains("OFFLINE")) return "DEVICE_OR_NETWORK";
        if (detectionType.contains("STREAM") || detectionType.contains("FRAME")) return "MEDIA_OR_NETWORK";
        if (detectionType.contains("RECORD") || detectionType.contains("STORAGE")) return "STORAGE";
        if (detectionType.contains("AI_SERVICE")) return "AI_SERVICE";
        if ("BLACK_SCREEN".equals(detectionType) || "FREEZE".equals(detectionType)
                || "BLUR".equals(detectionType) || "OCCLUSION".equals(detectionType)) return "CAMERA_OR_SCENE";
        return "UNKNOWN";
    }

    public InspectionReport report(String day) {
        String startTime = day + " 00:00:00";
        InspectionReport report = new InspectionReport();
        report.setTaskCount(mapper.taskCount(startTime));
        report.setCompletedCount(mapper.completedCount(startTime));
        report.setAbnormalCount(mapper.abnormalCount(startTime));
        report.setPendingCount(mapper.resultCount(startTime, "PENDING"));
        report.setConfirmedCount(mapper.resultCount(startTime, "CONFIRMED"));
        report.setFalsePositiveCount(mapper.resultCount(startTime, "FALSE_POSITIVE"));
        return report;
    }

    private String detectionName(String type) {
        if ("BLACK_SCREEN".equals(type)) return "画面黑屏";
        if ("FREEZE".equals(type)) return "画面冻结";
        if ("BLUR".equals(type)) return "画面模糊";
        if ("OCCLUSION".equals(type)) return "画面遮挡";
        return type;
    }

    public List<AiModel> models() {
        return mapper.models();
    }

    public AiModel createModel(AiModel model) {
        if (model.getName() == null || model.getVersion() == null || model.getCapabilities() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "模型名称、版本和能力不能为空");
        }
        model.setStatus("INACTIVE");
        model.setTrafficPercent(0);
        model.setCreateTime(DateUtil.getNow());
        mapper.insertModel(model);
        return model;
    }

    @Transactional
    public void activateModel(Integer id) {
        mapper.deactivateModels();
        if (mapper.activateModel(id) == 0) throw new ControllerException(ErrorCode.ERROR400.getCode(), "模型不存在");
    }

    public void rolloutModel(Integer id, Integer percent) {
        if (percent == null || percent < 0 || percent > 100) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "灰度比例必须在0到100之间");
        }
        if (mapper.rolloutModel(id, percent) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "模型不存在");
        }
    }

    public List<ModelQuality> modelQuality() {
        List<ModelQuality> metrics = mapper.modelQuality();
        for (ModelQuality metric : metrics) {
            int total = metric.getTotalCount() == null ? 0 : metric.getTotalCount();
            int confirmed = metric.getConfirmedCount() == null ? 0 : metric.getConfirmedCount();
            int falsePositive = metric.getFalsePositiveCount() == null ? 0 : metric.getFalsePositiveCount();
            metric.setConfirmationRate(total == 0 ? 0 : Math.round(confirmed * 1000.0 / total) / 1000.0);
            metric.setFalsePositiveRate(total == 0 ? 0 : Math.round(falsePositive * 1000.0 / total) / 1000.0);
            metric.setQualityStatus(total < 20 ? "INSUFFICIENT_DATA"
                    : metric.getFalsePositiveRate() > 0.3 ? "DRIFT_RISK" : "STABLE");
        }
        return metrics;
    }

    public List<SceneTemplate> sceneTemplates() {
        return mapper.sceneTemplates();
    }

    public List<SceneRegion> sceneRegions(Integer templateId) {
        if (mapper.sceneTemplate(templateId) == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "场景模板不存在");
        }
        return mapper.sceneRegions(templateId);
    }

    public SceneTemplate createSceneTemplate(SceneTemplate template) {
        if (template.getCode() == null || template.getCode().trim().isEmpty()
                || template.getName() == null || template.getName().trim().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "模板编码和名称不能为空");
        }
        template.setStatus("DRAFT");
        template.setVersion(1);
        template.setCreateTime(DateUtil.getNow());
        template.setUpdateTime(template.getCreateTime());
        mapper.insertSceneTemplate(template);
        return template;
    }

    public SceneRegion createSceneRegion(Integer templateId, SceneRegion region) {
        if (mapper.sceneTemplate(templateId) == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "场景模板不存在");
        }
        if (region.getName() == null || region.getRegionType() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "区域名称和类型不能为空");
        }
        region.setTemplateId(templateId);
        if (region.getActiveDays() == null) region.setActiveDays("1,2,3,4,5,6,7");
        if (region.getStartTime() == null) region.setStartTime("00:00");
        if (region.getEndTime() == null) region.setEndTime("23:59");
        if (region.getEnabled() == null) region.setEnabled(true);
        mapper.insertSceneRegion(region);
        return region;
    }

    @Transactional
    public SceneTemplate createFoodServicePreset() {
        SceneTemplate template = new SceneTemplate();
        template.setCode("FOOD_SERVICE");
        template.setName("餐饮后厨");
        template.setDescription("食品处理区域人员规范与环境卫生巡检预置模板");
        createSceneTemplate(template);
        createPresetRegion(template.getId(), "食品处理区", "FOOD_PROCESSING",
                "NO_WORK_CLOTHES,NO_WORK_CAP,SMOKING,PHONE_USE,BIN_UNCOVERED");
        createPresetRegion(template.getId(), "专间", "RESTRICTED_OPERATION",
                "NO_WORK_CLOTHES,NO_WORK_CAP,NO_MASK,PERSON_INTRUSION,DOOR_OPEN_TOO_LONG");
        createPresetRegion(template.getId(), "库房与垃圾区", "STORAGE_WASTE",
                "RODENT,ANIMAL_ENTRY,GARBAGE_OVERFLOW,FOOD_ON_FLOOR");
        return template;
    }

    private void createPresetRegion(Integer templateId, String name, String type, String algorithms) {
        SceneRegion region = new SceneRegion();
        region.setName(name);
        region.setRegionType(type);
        region.setAlgorithmCodes(algorithms);
        createSceneRegion(templateId, region);
    }

    public List<AlgorithmDefinition> algorithms() { return mapper.algorithms(); }

    @Transactional
    public int createPersonnelAlgorithms() {
        int count = 0;
        count += createAlgorithm("NO_WORK_CLOTHES", "未穿工作服", 3, 300, 0.80, "HIGH");
        count += createAlgorithm("NO_WORK_CAP", "未佩戴工作帽", 3, 300, 0.80, "HIGH");
        count += createAlgorithm("NO_MASK", "指定区域未佩戴口罩", 3, 300, 0.82, "HIGH");
        count += createAlgorithm("SMOKING", "吸烟", 2, 600, 0.88, "URGENT");
        count += createAlgorithm("PHONE_USE", "操作期间使用手机", 5, 300, 0.82, "NORMAL");
        count += createAlgorithm("PERSON_INTRUSION", "非授权人员进入", 3, 300, 0.85, "HIGH");
        return count;
    }

    @Transactional
    public int createEnvironmentAlgorithms() {
        int count = 0;
        count += createEnvironmentAlgorithm("BIN_UNCOVERED", "垃圾桶未加盖", 10, 0.82, "NORMAL");
        count += createEnvironmentAlgorithm("GARBAGE_OVERFLOW", "垃圾满溢", 10, 0.84, "HIGH");
        count += createEnvironmentAlgorithm("RODENT", "鼠类活动", 1, 0.88, "URGENT");
        count += createEnvironmentAlgorithm("ANIMAL_ENTRY", "动物进入", 2, 0.86, "HIGH");
        count += createEnvironmentAlgorithm("FOOD_ON_FLOOR", "食品落地存放", 10, 0.84, "HIGH");
        count += createEnvironmentAlgorithm("FLOOR_WATER", "地面积水", 15, 0.82, "NORMAL");
        count += createEnvironmentAlgorithm("DOOR_OPEN_TOO_LONG", "门长时间开启", 60, 0.80, "NORMAL");
        count += createEnvironmentAlgorithm("CAMERA_BLOCKED", "摄像头遮挡", 10, 0.90, "HIGH");
        return count;
    }

    private int createEnvironmentAlgorithm(String code, String name, int duration, double confidence, String risk) {
        if (mapper.algorithm(code) != null) return 0;
        AlgorithmDefinition definition = new AlgorithmDefinition();
        definition.setCode(code);
        definition.setName(name);
        definition.setCategory("ENVIRONMENT");
        definition.setMinDurationSeconds(duration);
        definition.setCooldownSeconds(300);
        definition.setConfidenceThreshold(confidence);
        definition.setRiskLevel(risk);
        definition.setEnabled(true);
        return mapper.insertAlgorithm(definition);
    }

    private int createAlgorithm(String code, String name, int duration, int cooldown,
                                double confidence, String risk) {
        if (mapper.algorithm(code) != null) return 0;
        AlgorithmDefinition definition = new AlgorithmDefinition();
        definition.setCode(code);
        definition.setName(name);
        definition.setCategory("PERSONNEL");
        definition.setMinDurationSeconds(duration);
        definition.setCooldownSeconds(cooldown);
        definition.setConfidenceThreshold(confidence);
        definition.setRiskLevel(risk);
        definition.setEnabled(true);
        return mapper.insertAlgorithm(definition);
    }

    public AlgorithmEvent receiveAlgorithmEvent(AlgorithmEvent event) {
        AlgorithmDefinition definition = mapper.algorithm(event.getAlgorithmCode());
        if (definition == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "算法未启用或不存在");
        }
        if (event.getEventUid() == null || event.getChannelId() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "事件编号和通道编号不能为空");
        }
        String target = event.getTargetId() == null ? "scene" : event.getTargetId();
        event.setDedupKey(event.getChannelId() + ":" + event.getAlgorithmCode() + ":" + target);
        AlgorithmEvent existing = mapper.openAlgorithmEvent(event.getDedupKey());
        if (existing != null) return existing;
        int duration = event.getDurationSeconds() == null ? 0 : event.getDurationSeconds();
        double confidence = event.getConfidence() == null ? 0 : event.getConfidence();
        event.setState(duration >= definition.getMinDurationSeconds()
                && confidence >= definition.getConfidenceThreshold() ? "OPEN" : "OBSERVING");
        event.setCreateTime(DateUtil.getNow());
        event.setOccurrenceCount(1);
        MaintenanceWindow window = mapper.activeMaintenanceWindow(event.getChannelId(),
                event.getRegionId() == null ? null : event.getRegionId().toString(), event.getCreateTime());
        if (window != null) {
            event.setState("SUPPRESSED");
            event.setSuppressedUntil(window.getEndTime());
        }
        mapper.insertAlgorithmEvent(event);
        return event;
    }

    public List<AlgorithmEvent> algorithmEvents() { return mapper.algorithmEvents(); }

    public int recoverAlgorithmEvent(Long id) {
        int updated = mapper.recoverAlgorithmEvent(id, DateUtil.getNow());
        if (updated == 0) throw new ControllerException(ErrorCode.ERROR400.getCode(), "仅待处理事件可以恢复");
        return updated;
    }

    public List<MaintenanceWindow> maintenanceWindows() { return mapper.maintenanceWindows(); }

    public MaintenanceWindow createMaintenanceWindow(MaintenanceWindow window) {
        if (window.getScopeType() == null || window.getScopeId() == null || window.getStartTime() == null
                || window.getEndTime() == null || window.getStartTime().compareTo(window.getEndTime()) >= 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "维护范围和有效时间不能为空，且结束时间须晚于开始时间");
        }
        if (!"CHANNEL".equals(window.getScopeType()) && !"REGION".equals(window.getScopeType())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "维护范围仅支持通道或区域");
        }
        if (window.getEnabled() == null) window.setEnabled(true);
        window.setCreateTime(DateUtil.getNow());
        mapper.insertMaintenanceWindow(window);
        return window;
    }

    public AlgorithmEvent reviewAlgorithmEvent(Long id, String status, String note, Integer userId) {
        if (!"CONFIRMED".equals(status) && !"FALSE_POSITIVE".equals(status)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "反馈状态不支持");
        }
        if (mapper.reviewAlgorithmEvent(id, status, userId, note, DateUtil.getNow()) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "事件不存在或已经反馈");
        }
        return mapper.algorithmEvent(id);
    }

    public List<SceneRiskSummary> sceneRiskSummaries() {
        List<SceneRiskSummary> summaries = mapper.sceneRiskSummaries();
        for (SceneRiskSummary summary : summaries) {
            int score = summary.getRiskScore() == null ? 0 : summary.getRiskScore();
            summary.setRiskLevel(score >= 30 ? "CRITICAL" : score >= 15 ? "HIGH" : score > 0 ? "NORMAL" : "LOW");
        }
        return summaries;
    }

    public List<MobileRecorder> mobileRecorders() { return mapper.mobileRecorders(); }

    public MobileRecorder createMobileRecorder(MobileRecorder recorder) {
        if (recorder.getDeviceCode() == null || recorder.getDeviceCode().trim().isEmpty()
                || recorder.getName() == null || recorder.getProtocolType() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备编号、名称和协议类型不能为空");
        }
        if (mapper.mobileRecorderByCode(recorder.getDeviceCode()) != null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备编号已经存在");
        }
        if (recorder.getCapabilities() == null) recorder.setCapabilities(defaultCapabilities(recorder.getProtocolType()));
        recorder.setStatus(recorder.getAssignedUserId() == null ? "AVAILABLE" : "ASSIGNED");
        recorder.setNetworkStatus("UNKNOWN");
        recorder.setCreateTime(DateUtil.getNow());
        recorder.setUpdateTime(recorder.getCreateTime());
        mapper.insertMobileRecorder(recorder);
        return recorder;
    }

    String defaultCapabilities(String protocol) {
        if ("GB28181".equals(protocol)) return "LIVE_VIDEO,AUDIO_LISTEN,LOCATION";
        if ("VENDOR_SDK".equals(protocol)) return "LIVE_VIDEO,AUDIO_LISTEN,INTERCOM,SNAPSHOT,RECORD_CONTROL,LOCATION,FILE_UPLOAD";
        return "LIVE_VIDEO,LOCATION,FILE_UPLOAD";
    }

    public MobileRecorder assignMobileRecorder(Long id, Integer userId) {
        MobileRecorder recorder = mapper.mobileRecorder(id);
        if (recorder == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "记录仪不存在");
        String status = userId == null ? "AVAILABLE" : "ASSIGNED";
        mapper.assignMobileRecorder(id, userId, status, DateUtil.getNow());
        recorder.setAssignedUserId(userId);
        recorder.setStatus(status);
        return recorder;
    }

    public RecorderLocation recordLocation(Long recorderId, RecorderLocation location) {
        if (mapper.mobileRecorder(recorderId) == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "记录仪不存在");
        }
        if (location.getLongitude() == null || location.getLatitude() == null
                || location.getLongitude() < -180 || location.getLongitude() > 180
                || location.getLatitude() < -90 || location.getLatitude() > 90) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "定位坐标不合法");
        }
        location.setRecorderId(recorderId);
        if (location.getCoordinateType() == null) location.setCoordinateType("GCJ02");
        if (location.getLocateTime() == null) location.setLocateTime(DateUtil.getNow());
        mapper.insertRecorderLocation(location);
        return location;
    }

    public List<RecorderLocation> recorderLocations(Long recorderId) { return mapper.recorderLocations(recorderId); }

    public List<StoreVisitTask> storeVisitTasks() { return mapper.storeVisitTasks(); }

    public StoreVisitTask createStoreVisitTask(StoreVisitTask task) {
        if (task.getStoreId() == null || task.getStoreName() == null || task.getAssigneeId() == null
                || task.getPlannedStartTime() == null || task.getPlannedEndTime() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "门店、执行人和计划时间不能为空");
        }
        if (task.getRecorderId() != null && mapper.mobileRecorder(task.getRecorderId()) == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "指定记录仪不存在");
        }
        task.setTaskCode("VISIT-" + System.currentTimeMillis());
        if (task.getTitle() == null) task.setTitle(task.getStoreName() + "巡店");
        task.setStatus("PENDING");
        task.setCreateTime(DateUtil.getNow());
        task.setUpdateTime(task.getCreateTime());
        mapper.insertStoreVisitTask(task);
        return task;
    }

    public StoreVisitTask checkinStoreVisitTask(Long id, double longitude, double latitude) {
        StoreVisitTask task = mapper.storeVisitTask(id);
        if (task == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡店任务不存在");
        if (task.getStoreLongitude() == null || task.getStoreLatitude() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "门店尚未配置定位");
        }
        double distance = distanceMeters(latitude, longitude, task.getStoreLatitude(), task.getStoreLongitude());
        if (distance > 500) throw new ControllerException(ErrorCode.ERROR400.getCode(), "当前位置距离门店过远，无法签到");
        if (mapper.checkinStoreVisitTask(id, distance, DateUtil.getNow()) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "任务状态不允许签到");
        }
        task.setStatus("IN_PROGRESS");
        task.setCheckinDistanceMeters(distance);
        return task;
    }

    double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double a = Math.sin((p2 - p1) / 2) * Math.sin((p2 - p1) / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(Math.toRadians(lon2 - lon1) / 2)
                * Math.sin(Math.toRadians(lon2 - lon1) / 2);
        return 6371000D * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public StoreVisitTask checkoutStoreVisitTask(Long id) {
        if (mapper.checkoutStoreVisitTask(id, DateUtil.getNow()) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "仅进行中的任务可以签退");
        }
        return mapper.storeVisitTask(id);
    }

    public VisitChecklistResult submitChecklistResult(Long taskId, VisitChecklistResult result, Integer userId) {
        StoreVisitTask task = mapper.storeVisitTask(taskId);
        if (task == null || !"IN_PROGRESS".equals(task.getStatus())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "仅进行中的巡店任务可以提交检查结果");
        }
        if (result.getSubmissionId() == null || result.getItemCode() == null || result.getResult() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "提交编号、检查项和结果不能为空");
        }
        VisitChecklistResult existing = mapper.checklistResultBySubmission(result.getSubmissionId());
        if (existing != null) return existing;
        if (Boolean.TRUE.equals(result.getEvidenceRequired()) &&
                (result.getEvidenceUrls() == null || result.getEvidenceUrls().trim().isEmpty())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "该检查项必须上传证据");
        }
        result.setTaskId(taskId);
        result.setSubmittedBy(userId);
        result.setSubmittedAt(DateUtil.getNow());
        mapper.insertChecklistResult(result);
        return result;
    }

    public List<VisitChecklistResult> checklistResults(Long taskId) { return mapper.checklistResults(taskId); }

    public void requireRecorderCapability(Long recorderId, String capability) {
        MobileRecorder recorder = mapper.mobileRecorder(recorderId);
        if (recorder == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "记录仪不存在");
        List<String> capabilities = Arrays.asList((recorder.getCapabilities() == null ? "" : recorder.getCapabilities()).split(","));
        if (!capabilities.contains(capability)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "当前设备协议不支持该操作");
        }
    }

    public StreamLease acquireStreamLease(String tenantId, Long recorderId, String businessType, int quota) {
        requireRecorderCapability(recorderId, "LIVE_VIDEO");
        if (tenantId == null || businessType == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "租户和取流业务类型不能为空");
        }
        int safeQuota = Math.max(1, quota);
        String now = DateUtil.getNow();
        if (mapper.activeStreamLeaseCount(tenantId, now) >= safeQuota) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "视频并发已达上限，请关闭其他预览后重试");
        }
        StreamLease lease = new StreamLease();
        lease.setTenantId(tenantId);
        lease.setRecorderId(recorderId);
        lease.setBusinessType(businessType);
        lease.setLeaseToken(UUID.randomUUID().toString());
        lease.setStatus("ACTIVE");
        lease.setCreateTime(now);
        lease.setExpiresAt(LocalDateTime.now().plusMinutes(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mapper.insertStreamLease(lease);
        return lease;
    }

    public int releaseStreamLease(String token) { return mapper.releaseStreamLease(token); }

    public VisitMediaFile registerVisitMedia(Long taskId, VisitMediaFile media, Integer userId) {
        if (mapper.storeVisitTask(taskId) == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡店任务不存在");
        }
        if (media.getUploadId() == null || media.getMediaType() == null || media.getFileName() == null
                || media.getFileSize() == null || media.getFileSize() <= 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "上传编号、文件类型、名称和大小不能为空");
        }
        VisitMediaFile existing = mapper.visitMediaByUploadId(media.getUploadId());
        if (existing != null) return existing;
        media.setTaskId(taskId);
        media.setUploadedBy(userId);
        media.setUploadedBytes(0);
        media.setStatus("PENDING");
        media.setCreateTime(DateUtil.getNow());
        media.setUpdateTime(media.getCreateTime());
        mapper.insertVisitMedia(media);
        return media;
    }

    public VisitMediaFile updateVisitMediaProgress(Long id, VisitMediaFile media) {
        media.setId(id);
        if (media.getUploadedBytes() == null || media.getUploadedBytes() < 0
                || media.getFileSize() != null && media.getUploadedBytes() > media.getFileSize()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "上传进度不合法");
        }
        if (media.getStatus() == null) media.setStatus("UPLOADING");
        media.setUpdateTime(DateUtil.getNow());
        mapper.updateVisitMediaProgress(media);
        return media;
    }

    public List<VisitMediaFile> visitMedia(Long taskId) { return mapper.visitMedia(taskId); }

    public VisitRectification createRectification(Long checkResultId, Integer assigneeId) {
        VisitChecklistResult result = mapper.checklistResult(checkResultId);
        if (result == null || !"FAIL".equals(result.getResult())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "仅不合格检查项可以生成整改任务");
        }
        StoreVisitTask task = mapper.storeVisitTask(result.getTaskId());
        if (task == null || assigneeId == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡店任务或整改负责人不存在");
        }
        VisitRectification rectification = new VisitRectification();
        rectification.setTaskId(task.getId());
        rectification.setCheckResultId(checkResultId);
        rectification.setStoreId(task.getStoreId());
        rectification.setStoreName(task.getStoreName());
        rectification.setTitle("巡店整改：" + (result.getItemName() == null ? result.getItemCode() : result.getItemName()));
        rectification.setSeverity(result.getSeverity() == null ? "NORMAL" : result.getSeverity());
        rectification.setAssigneeId(assigneeId);
        rectification.setStatus("OPEN");
        int hours = "URGENT".equals(rectification.getSeverity()) ? 2 : "HIGH".equals(rectification.getSeverity()) ? 8 : 24;
        rectification.setDueTime(LocalDateTime.now().plusHours(hours).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        rectification.setCreateTime(DateUtil.getNow());
        rectification.setUpdateTime(rectification.getCreateTime());
        mapper.insertVisitRectification(rectification);
        mapper.insertRectificationMessage(assigneeId, rectification.getTitle(), "请在截止时间前完成整改",
                rectification.getSeverity(), rectification.getId(), rectification.getCreateTime());
        return rectification;
    }

    public List<VisitRectification> visitRectifications() { return mapper.visitRectifications(); }

    public VisitRectification submitRectification(Long id, Integer userId, String resolution, String evidenceUrls) {
        if (resolution == null || resolution.trim().isEmpty() || evidenceUrls == null || evidenceUrls.trim().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "整改说明和整改证据不能为空");
        }
        if (mapper.submitRectification(id, userId, resolution, evidenceUrls, DateUtil.getNow()) == 0) {
            throw new ControllerException(ErrorCode.ERROR403.getCode(), "仅整改负责人可以提交");
        }
        return mapper.visitRectification(id);
    }

    public VisitRectification reviewRectification(Long id, boolean passed, Integer userId, String note) {
        String status = passed ? "CLOSED" : "REJECTED";
        if (mapper.reviewRectification(id, status, userId, note, DateUtil.getNow()) == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "仅待复核整改可以处理");
        }
        VisitRectification result = mapper.visitRectification(id);
        if (!passed && result != null) {
            mapper.insertRectificationMessage(result.getAssigneeId(), "整改被退回", note == null ? "请重新提交整改证据" : note,
                    "HIGH", id, DateUtil.getNow());
        }
        return result;
    }

    public VisitOperationsSummary visitOperations(int days) {
        int range = Math.max(1, Math.min(days, 365));
        String now = DateUtil.getNow();
        String since = LocalDateTime.now().minusDays(range - 1L).toLocalDate().atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        VisitOperationsSummary summary = mapper.visitOperationsSummary(since, now);
        if (summary == null) summary = new VisitOperationsSummary();
        summary.setCompletionRate(rate(summary.getCompletedCount(), summary.getTaskCount()));
        summary.setRectificationRate(rate(summary.getClosedCount(), summary.getRectificationCount()));
        return summary;
    }

    private double rate(Integer value, Integer total) {
        return total == null || total == 0 ? 0 : Math.round(value * 10000D / total) / 10000D;
    }

    public List<AiRule> rules() {
        return mapper.rules();
    }

    public AiRule createRule(AiRule rule) {
        if (rule.getName() == null || rule.getDetectionType() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "规则名称和检测类型不能为空");
        }
        if (rule.getConfidenceThreshold() == null || rule.getConfidenceThreshold() < 0 || rule.getConfidenceThreshold() > 1) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "置信度阈值必须在0到1之间");
        }
        rule.setCreateTime(DateUtil.getNow());
        rule.setUpdateTime(rule.getCreateTime());
        mapper.insertRule(rule);
        return rule;
    }

    public List<DetectionEffect> effects() {
        return mapper.effects();
    }

    public InspectionAnalytics analytics(int days) {
        int range = Math.max(1, Math.min(days, 90));
        String start = LocalDateTime.now().minusDays(range - 1L)
                .toLocalDate().atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        InspectionAnalytics analytics = new InspectionAnalytics();
        analytics.setDaily(mapper.dailyMetrics(start));
        analytics.setTopChannels(mapper.topChannels(start));
        return analytics;
    }

    public InspectionHealth health() {
        InspectionHealth health = new InspectionHealth();
        int count;
        try {
            count = mapper.schemaTableCount();
        } catch (RuntimeException error) {
            count = 0;
        }
        health.setTableCount(count);
        health.setMigrationReady(count == 12);
        health.setAiConfigured(aiClient.configured());
        health.setServiceUrl(properties.getServiceUrl());
        health.setStatus(!health.isMigrationReady() ? "MIGRATION_REQUIRED"
                : health.isAiConfigured() ? "READY" : "AI_NOT_CONFIGURED");
        return health;
    }

    public ChannelHealth recordHealth(ChannelHealth health) {
        if (health.getChannelId() == null || health.getChannelId().trim().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "通道编号不能为空");
        }
        int score = 100;
        if (!Boolean.TRUE.equals(health.getOnline())) score -= 60;
        if (!Boolean.TRUE.equals(health.getStreamAvailable())) score -= 25;
        int quality = health.getVideoQualityScore() == null ? 100
                : Math.max(0, Math.min(100, health.getVideoQualityScore()));
        score -= (100 - quality) / 4;
        if (!Boolean.TRUE.equals(health.getRecordingComplete())) score -= 10;
        if (health.getFirstFrameMillis() != null && health.getFirstFrameMillis() > 5000) score -= 5;
        score = Math.max(0, score);
        health.setVideoQualityScore(quality);
        health.setHealthScore(score);
        health.setHealthStatus(score >= 85 ? "HEALTHY" : score >= 60 ? "WARNING" : "CRITICAL");
        health.setCheckTime(DateUtil.getNow());
        mapper.insertChannelHealth(health);
        return health;
    }

    public HealthDashboard healthDashboard() {
        List<ChannelHealth> channels = mapper.latestChannelHealth();
        HealthDashboard dashboard = new HealthDashboard();
        dashboard.setTotal(channels.size());
        int totalScore = 0;
        java.util.ArrayList<ChannelHealth> problems = new java.util.ArrayList<>();
        for (ChannelHealth channel : channels) {
            totalScore += channel.getHealthScore() == null ? 0 : channel.getHealthScore();
            if ("HEALTHY".equals(channel.getHealthStatus())) dashboard.setHealthy(dashboard.getHealthy() + 1);
            else if ("WARNING".equals(channel.getHealthStatus())) dashboard.setWarning(dashboard.getWarning() + 1);
            else dashboard.setCritical(dashboard.getCritical() + 1);
            if (!"HEALTHY".equals(channel.getHealthStatus()) && problems.size() < 20) problems.add(channel);
        }
        dashboard.setAverageScore(channels.isEmpty() ? 0
                : Math.round(totalScore * 10.0 / channels.size()) / 10.0);
        dashboard.setProblemChannels(problems);
        return dashboard;
    }

    @Transactional
    public int cleanupTestData() {
        int deleted = mapper.deleteTestMessages();
        deleted += mapper.deleteTestAlarms();
        deleted += mapper.deleteTestWorkOrders();
        deleted += mapper.deleteTestChannelHealth();
        deleted += mapper.deleteTestResults();
        deleted += mapper.deleteTestTasks();
        deleted += mapper.deleteTestRules();
        deleted += mapper.deleteTestModels();
        deleted += mapper.deleteTestPlans();
        return deleted;
    }

    private InspectionPlan requiredPlan(Integer id) {
        InspectionPlan plan = mapper.plan(id);
        if (plan == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检计划不存在");
        return plan;
    }

    private int countChannels(String channelIds) {
        return channelIds == null || channelIds.trim().isEmpty() ? 0 : channelIds.split(",").length;
    }

    public boolean isWithinSchedule(InspectionPlan plan, LocalDateTime now) {
        String day = String.valueOf(now.getDayOfWeek().getValue());
        if (plan.getScheduleDays() != null
                && !java.util.Arrays.asList(plan.getScheduleDays().split(",")).contains(day)) {
            return false;
        }
        LocalTime current = now.toLocalTime();
        LocalTime start = LocalTime.parse(plan.getStartTime() == null ? "00:00" : plan.getStartTime());
        LocalTime end = LocalTime.parse(plan.getEndTime() == null ? "23:59" : plan.getEndTime());
        return !current.isBefore(start) && !current.isAfter(end);
    }

    public boolean isDue(InspectionPlan plan, LocalDateTime now) {
        String latest = mapper.latestTaskStart(plan.getId());
        if (latest == null || latest.trim().isEmpty()) return true;
        try {
            LocalDateTime lastRun = LocalDateTime.parse(latest,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            int interval = plan.getIntervalMinutes() == null ? 30 : plan.getIntervalMinutes();
            return !lastRun.plusMinutes(interval).isAfter(now);
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private void validatePlan(InspectionPlan plan) {
        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检计划名称不能为空");
        }
        if (plan.getIntervalMinutes() == null || plan.getIntervalMinutes() < 1) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检间隔不能小于1分钟");
        }
        if (plan.getDetectionTypes() == null || plan.getDetectionTypes().isEmpty()) {
            plan.setDetectionTypes("BLACK_SCREEN,FREEZE,BLUR,OCCLUSION");
        }
        if (plan.getScheduleDays() == null || plan.getScheduleDays().trim().isEmpty()) {
            plan.setScheduleDays("1,2,3,4,5,6,7");
        }
        if (plan.getStartTime() == null) plan.setStartTime("00:00");
        if (plan.getEndTime() == null) plan.setEndTime("23:59");
        try {
            LocalTime start = LocalTime.parse(plan.getStartTime());
            LocalTime end = LocalTime.parse(plan.getEndTime());
            if (end.isBefore(start)) throw new IllegalArgumentException();
        } catch (RuntimeException error) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检时间范围无效");
        }
    }

    private void updateTaskStatus(InspectionTask task, String status, String errorMessage) {
        task.setStatus(status);
        task.setErrorMessage(errorMessage);
        task.setEndTime("FAILED".equals(status) ? DateUtil.getNow() : null);
        task.setRetryCount(task.getRetryCount() == null ? 0 : task.getRetryCount());
        mapper.updateTaskStatus(task);
    }

    private String priority(Double confidence) {
        if (confidence == null) return "NORMAL";
        if (confidence >= 0.95) return "URGENT";
        if (confidence >= 0.85) return "HIGH";
        return "NORMAL";
    }
}
