package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Options;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备的报警信息
 */
@Mapper
@Repository
public interface DeviceAlarmMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO wvp_device_alarm (device_id, channel_id, alarm_priority, alarm_method, alarm_time, alarm_description, longitude, latitude, alarm_type, create_time, handling_status) " +
            "VALUES (#{deviceId}, #{channelId}, #{alarmPriority}, #{alarmMethod}, #{alarmTime}, #{alarmDescription}, #{longitude}, #{latitude}, #{alarmType}, #{createTime}, 'PENDING')")
    int add(DeviceAlarm alarm);


    @Select( value = {" <script>" +
            " SELECT a.*, u.username AS handled_by_name FROM wvp_device_alarm a LEFT JOIN wvp_user u ON a.handled_by = u.id " +
            " WHERE 1=1 " +
            " <if test=\"deviceId != null\" >  AND device_id = #{deviceId}</if>" +
            " <if test=\"channelId != null\" >  AND channel_id = #{channelId}</if>" +
            " <if test=\"alarmPriority != null\" >  AND alarm_priority = #{alarmPriority} </if>" +
            " <if test=\"alarmMethod != null\" >  AND alarm_method = #{alarmMethod} </if>" +
            " <if test=\"alarmType != null\" >  AND alarm_type = #{alarmType} </if>" +
            " <if test=\"startTime != null\" >  AND alarm_time &gt;= #{startTime} </if>" +
            " <if test=\"endTime != null\" >  AND alarm_time &lt;= #{endTime} </if>" +
            " ORDER BY alarm_time DESC " +
            " </script>"})
    List<DeviceAlarm> query(@Param("deviceId") String deviceId, @Param("channelId") String channelId, @Param("alarmPriority") String alarmPriority, @Param("alarmMethod") String alarmMethod,
                            @Param("alarmType") String alarmType, @Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("SELECT a.*, u.username AS handled_by_name FROM wvp_device_alarm a LEFT JOIN wvp_user u ON a.handled_by = u.id WHERE a.id = #{id}")
    DeviceAlarm getOne(@Param("id") Integer id);

    @Update("UPDATE wvp_device_alarm SET handling_status=#{status}, handled_by=#{userId}, handled_at=#{handledAt}, handling_note=#{note} WHERE id=#{id}")
    int handle(@Param("id") Integer id, @Param("status") String status, @Param("note") String note,
               @Param("userId") Integer userId, @Param("handledAt") String handledAt);


    @Delete(" <script>" +
            "DELETE FROM wvp_device_alarm WHERE 1=1 " +
            " <if test=\"deviceIdList != null and id == null \" > AND device_id in " +
            "<foreach collection='deviceIdList'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</if>" +
            " <if test=\"time != null and id == null \" > AND alarm_time &lt;= #{time}</if>" +
            " <if test=\"id != null\" > AND id = #{id}</if>" +
            " </script>"
            )
    int clearAlarmBeforeTime(@Param("id") Integer id, @Param("deviceIdList") List<String> deviceIdList, @Param("time") String time);
}
