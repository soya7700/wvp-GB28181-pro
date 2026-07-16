package com.genersoft.iot.vmp.vmanager.message.dao;

import com.genersoft.iot.vmp.vmanager.message.bean.SystemMessage;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SystemMessageMapper {

    @Insert("INSERT INTO wvp_system_message (user_id, type, title, content, level, business_type, business_id, read_flag, create_time) " +
            "SELECT id, 'ALARM', #{title}, #{content}, #{level}, 'DEVICE_ALARM', #{alarmId}, false, #{createTime} FROM wvp_user")
    int insertAlarmForAllUsers(@Param("alarmId") Integer alarmId, @Param("title") String title,
                               @Param("content") String content, @Param("level") String level,
                               @Param("createTime") String createTime);

    @Select({"<script>",
            "SELECT m.*, a.handling_status AS alarm_status, a.device_id, a.channel_id FROM wvp_system_message m ",
            "LEFT JOIN wvp_device_alarm a ON m.business_type='DEVICE_ALARM' AND m.business_id=a.id ",
            "WHERE m.user_id=#{userId} ",
            "<if test='readFlag != null'>AND m.read_flag=#{readFlag}</if>",
            "<if test='type != null and type != \"\"'>AND m.type=#{type}</if>",
            "ORDER BY m.create_time DESC",
            "</script>"})
    List<SystemMessage> query(@Param("userId") Integer userId, @Param("readFlag") Boolean readFlag, @Param("type") String type);

    @Select("SELECT m.*, a.handling_status AS alarm_status, a.device_id, a.channel_id FROM wvp_system_message m " +
            "LEFT JOIN wvp_device_alarm a ON m.business_type='DEVICE_ALARM' AND m.business_id=a.id " +
            "WHERE m.id=#{id} AND m.user_id=#{userId}")
    SystemMessage getOne(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT COUNT(0) FROM wvp_system_message WHERE user_id=#{userId} AND read_flag=false")
    int unreadCount(@Param("userId") Integer userId);

    @Update("UPDATE wvp_system_message SET read_flag=true, read_time=#{readTime} WHERE id=#{id} AND user_id=#{userId}")
    int markRead(@Param("id") Integer id, @Param("userId") Integer userId, @Param("readTime") String readTime);

    @Update("UPDATE wvp_system_message SET read_flag=true, read_time=#{readTime} WHERE user_id=#{userId} AND read_flag=false")
    int markAllRead(@Param("userId") Integer userId, @Param("readTime") String readTime);

    @Update("UPDATE wvp_system_message SET read_flag=true, read_time=#{readTime} WHERE user_id=#{userId} AND business_type='DEVICE_ALARM' AND business_id=#{alarmId}")
    int markAlarmRead(@Param("alarmId") Integer alarmId, @Param("userId") Integer userId, @Param("readTime") String readTime);
}
