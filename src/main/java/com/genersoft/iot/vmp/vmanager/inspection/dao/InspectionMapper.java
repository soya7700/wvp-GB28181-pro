package com.genersoft.iot.vmp.vmanager.inspection.dao;

import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionTask;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiModel;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiRule;
import com.genersoft.iot.vmp.vmanager.inspection.bean.DetectionEffect;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InspectionMapper {

    @Select("SELECT * FROM wvp_ai_inspection_plan ORDER BY id DESC")
    List<InspectionPlan> plans();

    @Select("SELECT * FROM wvp_ai_inspection_plan WHERE id=#{id}")
    InspectionPlan plan(Integer id);

    @Select("SELECT p.* FROM wvp_ai_inspection_plan p WHERE p.enabled=true AND NOT EXISTS (" +
            "SELECT 1 FROM wvp_ai_inspection_task t WHERE t.plan_id=p.id " +
            "AND t.start_time >= DATE_FORMAT(DATE_SUB(NOW(), INTERVAL p.interval_minutes MINUTE), '%Y-%m-%d %H:%i:%s'))")
    List<InspectionPlan> duePlans();

    @Insert("INSERT INTO wvp_ai_inspection_plan(name,enabled,interval_minutes,detection_types,channel_ids,create_time,update_time) " +
            "VALUES(#{name},#{enabled},#{intervalMinutes},#{detectionTypes},#{channelIds},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPlan(InspectionPlan plan);

    @Update("UPDATE wvp_ai_inspection_plan SET enabled=#{enabled},update_time=#{updateTime} WHERE id=#{id}")
    int togglePlan(InspectionPlan plan);

    @Insert("INSERT INTO wvp_ai_inspection_task(plan_id,status,channel_total,success_count,abnormal_count,start_time,retry_count) " +
            "VALUES(#{planId},#{status},#{channelTotal},0,0,#{startTime},0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTask(InspectionTask task);

    @Select("SELECT t.*,p.name AS plan_name FROM wvp_ai_inspection_task t LEFT JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id ORDER BY t.id DESC")
    List<InspectionTask> tasks();

    @Insert("INSERT INTO wvp_ai_inspection_result(callback_id,task_id,device_id,channel_id,detection_type,confidence,status,evidence_url,marked_url,create_time) " +
            "VALUES(#{callbackId},#{taskId},#{deviceId},#{channelId},#{detectionType},#{confidence},#{status},#{evidenceUrl},#{markedUrl},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertResult(InspectionResult result);

    @Select("<script>SELECT * FROM wvp_ai_inspection_result <where><if test='status != null'>status=#{status}</if></where> ORDER BY id DESC</script>")
    List<InspectionResult> results(@Param("status") String status);

    @Select("SELECT * FROM wvp_ai_inspection_result WHERE id=#{id}")
    InspectionResult result(Long id);

    @Select("SELECT * FROM wvp_ai_inspection_result WHERE callback_id=#{callbackId}")
    InspectionResult resultByCallbackId(String callbackId);

    @Update("UPDATE wvp_ai_inspection_result SET status=#{status},review_note=#{reviewNote},reviewed_by=#{reviewedBy},reviewed_at=#{reviewedAt},alarm_id=#{alarmId} WHERE id=#{id} AND status='PENDING'")
    int review(InspectionResult result);

    @Select("SELECT COUNT(0) FROM wvp_ai_inspection_task WHERE start_time >=#{startTime}")
    int taskCount(@Param("startTime") String startTime);

    @Select("SELECT COUNT(0) FROM wvp_ai_inspection_task WHERE start_time >=#{startTime} AND status='COMPLETED'")
    int completedCount(@Param("startTime") String startTime);

    @Select("SELECT COUNT(0) FROM wvp_ai_inspection_result WHERE create_time >=#{startTime}")
    int abnormalCount(@Param("startTime") String startTime);

    @Select("SELECT COUNT(0) FROM wvp_ai_inspection_result WHERE create_time >=#{startTime} AND status=#{status}")
    int resultCount(@Param("startTime") String startTime, @Param("status") String status);

    @Select("SELECT * FROM wvp_ai_model ORDER BY id DESC")
    List<AiModel> models();

    @Insert("INSERT INTO wvp_ai_model(name,version,capabilities,status,service_endpoint,create_time) VALUES(#{name},#{version},#{capabilities},#{status},#{serviceEndpoint},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertModel(AiModel model);

    @Update("UPDATE wvp_ai_model SET status='INACTIVE' WHERE status='ACTIVE'")
    int deactivateModels();

    @Update("UPDATE wvp_ai_model SET status='ACTIVE' WHERE id=#{id}")
    int activateModel(Integer id);

    @Select("SELECT * FROM wvp_ai_rule ORDER BY id DESC")
    List<AiRule> rules();

    @Insert("INSERT INTO wvp_ai_rule(name,plan_id,detection_type,confidence_threshold,region_points,enabled,create_time,update_time) VALUES(#{name},#{planId},#{detectionType},#{confidenceThreshold},#{regionPoints},#{enabled},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRule(AiRule rule);

    @Select("SELECT detection_type,COUNT(0) total_count,SUM(CASE WHEN status='CONFIRMED' THEN 1 ELSE 0 END) confirmed_count,SUM(CASE WHEN status='FALSE_POSITIVE' THEN 1 ELSE 0 END) false_positive_count FROM wvp_ai_inspection_result GROUP BY detection_type ORDER BY total_count DESC")
    List<DetectionEffect> effects();

    @Update("UPDATE wvp_ai_inspection_task SET status=#{status},success_count=#{successCount},abnormal_count=#{abnormalCount},end_time=#{endTime},error_message=#{errorMessage} WHERE id=#{id}")
    int completeTask(InspectionTask task);

    @Update("UPDATE wvp_ai_inspection_task SET status=#{status},error_message=#{errorMessage},end_time=#{endTime},retry_count=#{retryCount} WHERE id=#{id}")
    int updateTaskStatus(InspectionTask task);
}
