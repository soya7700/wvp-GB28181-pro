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
        model.setCreateTime(DateUtil.getNow());
        mapper.insertModel(model);
        return model;
    }

    @Transactional
    public void activateModel(Integer id) {
        mapper.deactivateModels();
        if (mapper.activateModel(id) == 0) throw new ControllerException(ErrorCode.ERROR400.getCode(), "模型不存在");
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
        health.setMigrationReady(count == 5);
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
