package com.genersoft.iot.vmp.vmanager.message.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.message.bean.SystemMessage;
import com.genersoft.iot.vmp.vmanager.message.dao.SystemMessageMapper;
import com.genersoft.iot.vmp.vmanager.message.service.ISystemMessageService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemMessageServiceImpl implements ISystemMessageService {
    @Autowired
    private SystemMessageMapper mapper;

    @Override
    public void createAlarmMessages(Integer alarmId, DeviceAlarm alarm) {
        String title = alarm.getAlarmDescription();
        if (title == null || title.trim().isEmpty()) {
            title = "设备告警";
        }
        String content = "设备：" + alarm.getDeviceId() + "，通道：" + alarm.getChannelId() + "，时间：" + alarm.getAlarmTime();
        mapper.insertAlarmForAllUsers(alarmId, title, content, alarm.getAlarmPriority(), DateUtil.getNow());
    }

    @Override
    public PageInfo<SystemMessage> query(Integer userId, int page, int count, Boolean readFlag, String type) {
        PageHelper.startPage(page, count);
        List<SystemMessage> list = mapper.query(userId, readFlag, type);
        return new PageInfo<>(list);
    }

    @Override
    public SystemMessage getOne(Integer id, Integer userId) {
        return mapper.getOne(id, userId);
    }

    @Override
    public int unreadCount(Integer userId) {
        return mapper.unreadCount(userId);
    }

    @Override
    public void markRead(Integer id, Integer userId) {
        mapper.markRead(id, userId, DateUtil.getNow());
    }

    @Override
    public void markAllRead(Integer userId) {
        mapper.markAllRead(userId, DateUtil.getNow());
    }

    @Override
    public void markAlarmRead(Integer alarmId, Integer userId) {
        mapper.markAlarmRead(alarmId, userId, DateUtil.getNow());
    }
}
