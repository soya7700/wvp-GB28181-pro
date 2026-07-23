package com.genersoft.iot.vmp.vmanager.inspection.service;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionTask;
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

    private InspectionPlan requiredPlan(Integer id) {
        InspectionPlan plan = mapper.plan(id);
        if (plan == null) throw new ControllerException(ErrorCode.ERROR400.getCode(), "巡检计划不存在");
        return plan;
    }

    private int countChannels(String channelIds) {
        return channelIds == null || channelIds.trim().isEmpty() ? 0 : channelIds.split(",").length;
    }
}
