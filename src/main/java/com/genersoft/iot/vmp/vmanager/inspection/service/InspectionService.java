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
import com.genersoft.iot.vmp.vmanager.inspection.dao.InspectionMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class InspectionService {
    @Autowired
    private InspectionMapper mapper;

    @Autowired
    private IDeviceAlarmService alarmService;

    @Autowired
    private AiInspectionClient aiClient;

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
        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检计划名称不能为空");
        }
        if (plan.getIntervalMinutes() == null || plan.getIntervalMinutes() < 1) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检间隔不能小于1分钟");
        }
        if (plan.getDetectionTypes() == null || plan.getDetectionTypes().isEmpty()) {
            plan.setDetectionTypes("BLACK_SCREEN,FREEZE,BLUR,OCCLUSION");
        }
        plan.setCreateTime(DateUtil.getNow());
        plan.setUpdateTime(plan.getCreateTime());
        mapper.insertPlan(plan);
        return plan;
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
            try {
                aiClient.dispatch(plan, task);
                updateTaskStatus(task, "WAITING_AI", null);
            } catch (RuntimeException error) {
                updateTaskStatus(task, "FAILED", "AI服务派发失败：" + error.getMessage());
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
        result.setTaskId(taskId);
        result.setStatus("PENDING");
        result.setCreateTime(DateUtil.getNow());
        mapper.insertResult(result);
        return result;
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
        return result;
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

    private InspectionPlan requiredPlan(Integer id) {
        InspectionPlan plan = mapper.plan(id);
        if (plan == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检计划不存在");
        return plan;
    }

    private int countChannels(String channelIds) {
        return channelIds == null || channelIds.trim().isEmpty() ? 0 : channelIds.split(",").length;
    }

    private void updateTaskStatus(InspectionTask task, String status, String errorMessage) {
        task.setStatus(status);
        task.setErrorMessage(errorMessage);
        task.setEndTime("FAILED".equals(status) ? DateUtil.getNow() : null);
        task.setRetryCount(task.getRetryCount() == null ? 0 : task.getRetryCount());
        mapper.updateTaskStatus(task);
    }
}
