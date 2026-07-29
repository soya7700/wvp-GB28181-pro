package com.genersoft.iot.vmp.vmanager.message.service;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.vmanager.message.bean.SystemMessage;
import com.github.pagehelper.PageInfo;

public interface ISystemMessageService {
    void createAlarmMessages(Integer alarmId, DeviceAlarm alarm);
    PageInfo<SystemMessage> query(Integer userId, int page, int count, Boolean readFlag, String type);
    SystemMessage getOne(Integer id, Integer userId);
    int unreadCount(Integer userId);
    void markRead(Integer id, Integer userId);
    void markAllRead(Integer userId);
    void markAlarmRead(Integer alarmId, Integer userId);
}
