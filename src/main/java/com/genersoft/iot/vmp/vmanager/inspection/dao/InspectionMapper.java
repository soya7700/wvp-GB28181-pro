package com.genersoft.iot.vmp.vmanager.inspection.dao;

import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionTask;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiModel;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiRule;
import com.genersoft.iot.vmp.vmanager.inspection.bean.DetectionEffect;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionAnalytics;
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

    @Insert("INSERT INTO wvp_ai_inspection_plan(name,enabled,interval_minutes,detection_types,channel_ids,schedule_days,start_time,end_time,create_time,update_time) " +
            "VALUES(#{name},#{enabled},#{intervalMinutes},#{detectionTypes},#{channelIds},#{scheduleDays},#{startTime},#{endTime},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPlan(InspectionPlan plan);

    @Update("UPDATE wvp_ai_inspection_plan SET name=#{name},enabled=#{enabled},interval_minutes=#{intervalMinutes}," +
            "detection_types=#{detectionTypes},channel_ids=#{channelIds},schedule_days=#{scheduleDays}," +
            "start_time=#{startTime},end_time=#{endTime},update_time=#{updateTime} WHERE id=#{id}")
    int updatePlan(InspectionPlan plan);

    @Delete("DELETE FROM wvp_ai_inspection_plan WHERE id=#{id}")
    int deletePlan(Integer id);

    @Update("UPDATE wvp_ai_inspection_plan SET enabled=#{enabled},update_time=#{updateTime} WHERE id=#{id}")
    int togglePlan(InspectionPlan plan);

    @Insert("INSERT INTO wvp_ai_inspection_task(plan_id,status,channel_total,success_count,abnormal_count,start_time,retry_count) " +
            "VALUES(#{planId},#{status},#{channelTotal},0,0,#{startTime},0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTask(InspectionTask task);

    @Select("SELECT t.*,p.name AS plan_name FROM wvp_ai_inspection_task t LEFT JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id ORDER BY t.id DESC")
    List<InspectionTask> tasks();

    @Insert("INSERT INTO wvp_ai_inspection_result(callback_id,task_id,device_id,channel_id,detection_type,confidence,status,workflow_status,priority,occurrence_count,evidence_url,marked_url,create_time) " +
            "VALUES(#{callbackId},#{taskId},#{deviceId},#{channelId},#{detectionType},#{confidence},#{status},#{workflowStatus},#{priority},1,#{evidenceUrl},#{markedUrl},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertResult(InspectionResult result);

    @Select("<script>SELECT * FROM wvp_ai_inspection_result <where><if test='status != null'>status=#{status}</if></where> ORDER BY id DESC</script>")
    List<InspectionResult> results(@Param("status") String status);

    @Select("SELECT * FROM wvp_ai_inspection_result WHERE id=#{id}")
    InspectionResult result(Long id);

    @Select("SELECT * FROM wvp_ai_inspection_result WHERE callback_id=#{callbackId}")
    InspectionResult resultByCallbackId(String callbackId);

    @Select("SELECT * FROM wvp_ai_inspection_result WHERE channel_id=#{channelId} AND detection_type=#{detectionType} " +
            "AND workflow_status!='CLOSED' AND create_time>=#{since} ORDER BY id DESC LIMIT 1")
    InspectionResult recentOpenResult(@Param("channelId") String channelId,
                                      @Param("detectionType") String detectionType,
                                      @Param("since") String since);

    @Update("UPDATE wvp_ai_inspection_result SET occurrence_count=occurrence_count+1,confidence=GREATEST(confidence,#{confidence})," +
            "evidence_url=COALESCE(#{evidenceUrl},evidence_url),marked_url=COALESCE(#{markedUrl},marked_url) WHERE id=#{id}")
    int mergeResult(InspectionResult result);

    @Update("UPDATE wvp_ai_inspection_result SET assignee_id=#{userId},workflow_status='CLAIMED' " +
            "WHERE id=#{id} AND assignee_id IS NULL AND workflow_status!='CLOSED'")
    int claimResult(@Param("id") Long id, @Param("userId") Integer userId);

    @Update("UPDATE wvp_ai_inspection_result SET assignee_id=#{userId},workflow_status='CLAIMED' " +
            "WHERE id=#{id} AND workflow_status!='CLOSED'")
    int assignResult(@Param("id") Long id, @Param("userId") Integer userId);

    @Update("UPDATE wvp_ai_inspection_result SET workflow_status=#{workflowStatus},handling_note=#{handlingNote}," +
            "handled_at=#{handledAt} WHERE id=#{id} AND assignee_id=#{assigneeId} AND workflow_status!='CLOSED'")
    int handleResult(InspectionResult result);

    @Update("UPDATE wvp_ai_inspection_result SET status=#{status},workflow_status='CLOSED',review_note=#{reviewNote},reviewed_by=#{reviewedBy},reviewed_at=#{reviewedAt},handled_at=#{reviewedAt},alarm_id=#{alarmId} WHERE id=#{id} AND status='PENDING'")
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

    @Select("SELECT DATE(start_time) day,COUNT(0) task_count,SUM(abnormal_count) abnormal_count," +
            "(SELECT COUNT(0) FROM wvp_ai_inspection_result r WHERE DATE(r.create_time)=DATE(t.start_time) AND r.status='CONFIRMED') confirmed_count " +
            "FROM wvp_ai_inspection_task t WHERE start_time>=#{startTime} GROUP BY DATE(start_time) ORDER BY day")
    List<InspectionAnalytics.DailyMetric> dailyMetrics(@Param("startTime") String startTime);

    @Select("SELECT channel_id,COUNT(0) abnormal_count,SUM(CASE WHEN status='CONFIRMED' THEN 1 ELSE 0 END) confirmed_count " +
            "FROM wvp_ai_inspection_result WHERE create_time>=#{startTime} GROUP BY channel_id ORDER BY abnormal_count DESC LIMIT 10")
    List<InspectionAnalytics.ChannelMetric> topChannels(@Param("startTime") String startTime);

    @Select("SELECT COUNT(0) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN " +
            "('wvp_ai_inspection_plan','wvp_ai_inspection_task','wvp_ai_inspection_result','wvp_ai_model','wvp_ai_rule')")
    int schemaTableCount();

    @Delete("DELETE r FROM wvp_ai_inspection_result r JOIN wvp_ai_inspection_task t ON r.task_id=t.id " +
            "JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id WHERE p.name LIKE 'CODEX-TEST-%'")
    int deleteTestResults();

    @Delete("DELETE t FROM wvp_ai_inspection_task t JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id WHERE p.name LIKE 'CODEX-TEST-%'")
    int deleteTestTasks();

    @Delete("DELETE FROM wvp_ai_rule WHERE name LIKE 'CODEX-TEST-%'")
    int deleteTestRules();

    @Delete("DELETE FROM wvp_ai_model WHERE name LIKE 'CODEX-TEST-%'")
    int deleteTestModels();

    @Delete("DELETE FROM wvp_ai_inspection_plan WHERE name LIKE 'CODEX-TEST-%'")
    int deleteTestPlans();
}
