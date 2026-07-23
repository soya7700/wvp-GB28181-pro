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
import com.genersoft.iot.vmp.vmanager.inspection.dao.InspectionMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InspectionService {
    @Autowired
    private InspectionMapper mapper;

    @Autowired
    private IDeviceAlarmService alarmService;

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
        task.setStatus("WAITING_AI");
        task.setChannelTotal(countChannels(plan.getChannelIds()));
        task.setStartTime(DateUtil.getNow());
        mapper.insertTask(task);
        return task;
    }

    @Transactional
    public InspectionResult addResult(Long taskId, InspectionResult result) {
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
            DeviceAlarm alarm = new DeviceAlarm();
            alarm.setDeviceId(result.getDeviceId());
            alarm.setChannelId(result.getChannelId());
            alarm.setAlarmPriority("2");
            alarm.setAlarmMethod("5");
            alarm.setAlarmType(result.getDetectionType());
            alarm.setAlarmTime(DateUtil.getNow());
            alarm.setAlarmDescription("AI巡检异常：" + detectionName(result.getDetectionType()));
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

    private InspectionPlan requiredPlan(Integer id) {
        InspectionPlan plan = mapper.plan(id);
        if (plan == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检计划不存在");
        return plan;
    }

    private int countChannels(String channelIds) {
        return channelIds == null || channelIds.trim().isEmpty() ? 0 : channelIds.split(",").length;
    }
}
