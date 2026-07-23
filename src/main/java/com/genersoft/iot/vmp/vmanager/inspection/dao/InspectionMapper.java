package com.genersoft.iot.vmp.vmanager.inspection.dao;

import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InspectionMapper {

    @Select("SELECT * FROM wvp_ai_inspection_plan ORDER BY id DESC")
    List<InspectionPlan> plans();

    @Select("SELECT * FROM wvp_ai_inspection_plan WHERE id=#{id}")
    InspectionPlan plan(Integer id);

    @Insert("INSERT INTO wvp_ai_inspection_plan(name,enabled,interval_minutes,detection_types,channel_ids,create_time,update_time) " +
            "VALUES(#{name},#{enabled},#{intervalMinutes},#{detectionTypes},#{channelIds},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPlan(InspectionPlan plan);

    @Update("UPDATE wvp_ai_inspection_plan SET enabled=#{enabled},update_time=#{updateTime} WHERE id=#{id}")
    int togglePlan(InspectionPlan plan);

    @Insert("INSERT INTO wvp_ai_inspection_task(plan_id,status,channel_total,success_count,abnormal_count,start_time) " +
            "VALUES(#{planId},#{status},#{channelTotal},0,0,#{startTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTask(InspectionTask task);

    @Select("SELECT t.*,p.name AS plan_name FROM wvp_ai_inspection_task t LEFT JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id ORDER BY t.id DESC")
    List<InspectionTask> tasks();

    @Insert("INSERT INTO wvp_ai_inspection_result(task_id,device_id,channel_id,detection_type,confidence,status,evidence_url,marked_url,create_time) " +
            "VALUES(#{taskId},#{deviceId},#{channelId},#{detectionType},#{confidence},#{status},#{evidenceUrl},#{markedUrl},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertResult(InspectionResult result);

    @Select("<script>SELECT * FROM wvp_ai_inspection_result <where><if test='status != null'>status=#{status}</if></where> ORDER BY id DESC</script>")
    List<InspectionResult> results(@Param("status") String status);

    @Update("UPDATE wvp_ai_inspection_task SET status=#{status},success_count=#{successCount},abnormal_count=#{abnormalCount},end_time=#{endTime},error_message=#{errorMessage} WHERE id=#{id}")
    int completeTask(InspectionTask task);
}
