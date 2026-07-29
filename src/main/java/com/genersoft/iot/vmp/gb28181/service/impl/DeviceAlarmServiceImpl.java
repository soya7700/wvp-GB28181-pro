package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.dao.DeviceAlarmMapper;
import com.genersoft.iot.vmp.gb28181.service.IDeviceAlarmService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.genersoft.iot.vmp.vmanager.message.service.ISystemMessageService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceAlarmServiceImpl implements IDeviceAlarmService {

    @Autowired
    private DeviceAlarmMapper deviceAlarmMapper;

    @Autowired
    private ISystemMessageService systemMessageService;

    @Override
    public PageInfo<DeviceAlarm> getAllAlarm(int page, int count, String deviceId, String channelId, String alarmPriority, String alarmMethod, String alarmType, String startTime, String endTime) {
        PageHelper.startPage(page, count);
        List<DeviceAlarm> all = deviceAlarmMapper.query(deviceId, channelId, alarmPriority, alarmMethod, alarmType, startTime, endTime);
        return new PageInfo<>(all);
    }

    @Override
    public void add(DeviceAlarm deviceAlarm) {
        deviceAlarmMapper.add(deviceAlarm);
        if (deviceAlarm.getId() != null) {
            systemMessageService.createAlarmMessages(Integer.parseInt(deviceAlarm.getId()), deviceAlarm);
        }
    }

    @Override
    public DeviceAlarm getOne(Integer id) {
        return deviceAlarmMapper.getOne(id);
    }

    @Override
    public DeviceAlarm handle(Integer id, String status, String note, Integer userId) {
        if (!"ACKNOWLEDGED".equals(status) && !"RESOLVED".equals(status) && !"FALSE_ALARM".equals(status)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "不支持的处置状态");
        }
        if (deviceAlarmMapper.getOne(id) == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "告警不存在");
        }
        deviceAlarmMapper.handle(id, status, note, userId, DateUtil.getNow());
        systemMessageService.markAlarmRead(id, userId);
        return deviceAlarmMapper.getOne(id);
    }

    @Override
    public int clearAlarmBeforeTime(Integer id, List<String> deviceIdList, String time) {
        return deviceAlarmMapper.clearAlarmBeforeTime(id, deviceIdList, time);
    }
}
