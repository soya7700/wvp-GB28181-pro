package com.genersoft.iot.vmp.vmanager.inspection.dao;

import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionResult;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionTask;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiModel;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AiRule;
import com.genersoft.iot.vmp.vmanager.inspection.bean.DetectionEffect;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionAnalytics;
import com.genersoft.iot.vmp.vmanager.inspection.bean.ChannelHealth;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionWorkOrder;
import com.genersoft.iot.vmp.vmanager.inspection.bean.IncidentGroup;
import com.genersoft.iot.vmp.vmanager.inspection.bean.ModelQuality;
import com.genersoft.iot.vmp.vmanager.inspection.bean.SceneTemplate;
import com.genersoft.iot.vmp.vmanager.inspection.bean.SceneRegion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InspectionMapper {

    @Select("SELECT * FROM wvp_ai_inspection_plan ORDER BY id DESC")
    List<InspectionPlan> plans();

    @Select("SELECT * FROM wvp_ai_inspection_plan WHERE id=#{id}")
    InspectionPlan plan(Integer id);

    @Select("SELECT * FROM wvp_ai_inspection_plan WHERE enabled=true")
    List<InspectionPlan> duePlans();

    @Select("SELECT MAX(start_time) FROM wvp_ai_inspection_task WHERE plan_id=#{planId}")
    String latestTaskStart(Integer planId);

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

    @Insert("INSERT INTO wvp_ai_inspection_result(callback_id,task_id,device_id,channel_id,detection_type,confidence,status,workflow_status,priority,occurrence_count,evidence_url,marked_url,create_time,aggregation_key,root_cause,model_id,rule_id) " +
            "VALUES(#{callbackId},#{taskId},#{deviceId},#{channelId},#{detectionType},#{confidence},#{status},#{workflowStatus},#{priority},1,#{evidenceUrl},#{markedUrl},#{createTime},#{aggregationKey},#{rootCause},#{modelId},#{ruleId})")
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

    @Select("SELECT * FROM wvp_ai_inspection_result WHERE aggregation_key=#{aggregationKey} " +
            "AND workflow_status!='CLOSED' AND create_time>=#{since} ORDER BY id DESC LIMIT 1")
    InspectionResult recentIncident(@Param("aggregationKey") String aggregationKey, @Param("since") String since);

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

    @Insert("INSERT INTO wvp_ai_model(name,version,capabilities,status,service_endpoint,traffic_percent,create_time) VALUES(#{name},#{version},#{capabilities},#{status},#{serviceEndpoint},#{trafficPercent},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertModel(AiModel model);

    @Update("UPDATE wvp_ai_model SET status='INACTIVE',traffic_percent=0 WHERE status='ACTIVE'")
    int deactivateModels();

    @Update("UPDATE wvp_ai_model SET status='ACTIVE',traffic_percent=100 WHERE id=#{id}")
    int activateModel(Integer id);

    @Update("UPDATE wvp_ai_model SET traffic_percent=#{percent},status=CASE WHEN #{percent}>0 THEN 'CANARY' ELSE 'INACTIVE' END WHERE id=#{id}")
    int rolloutModel(@Param("id") Integer id, @Param("percent") Integer percent);

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

    @Select("SELECT task_daily.day,task_daily.task_count,task_daily.abnormal_count," +
            "COALESCE(result_daily.confirmed_count,0) confirmed_count FROM (" +
            "SELECT DATE(start_time) day,COUNT(0) task_count,COALESCE(SUM(abnormal_count),0) abnormal_count " +
            "FROM wvp_ai_inspection_task WHERE start_time>=#{startTime} GROUP BY DATE(start_time)" +
            ") task_daily LEFT JOIN (" +
            "SELECT DATE(create_time) day,COUNT(0) confirmed_count FROM wvp_ai_inspection_result " +
            "WHERE create_time>=#{startTime} AND status='CONFIRMED' GROUP BY DATE(create_time)" +
            ") result_daily ON result_daily.day=task_daily.day ORDER BY task_daily.day")
    List<InspectionAnalytics.DailyMetric> dailyMetrics(@Param("startTime") String startTime);

    @Select("SELECT channel_id,COUNT(0) abnormal_count,SUM(CASE WHEN status='CONFIRMED' THEN 1 ELSE 0 END) confirmed_count " +
            "FROM wvp_ai_inspection_result WHERE create_time>=#{startTime} GROUP BY channel_id ORDER BY abnormal_count DESC LIMIT 10")
    List<InspectionAnalytics.ChannelMetric> topChannels(@Param("startTime") String startTime);

    @Select("SELECT COUNT(0) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN " +
            "('wvp_ai_inspection_plan','wvp_ai_inspection_task','wvp_ai_inspection_result','wvp_ai_model','wvp_ai_rule'," +
            "'wvp_ai_channel_health','wvp_ai_work_order','wvp_ai_scene_template','wvp_ai_scene_region')")
    int schemaTableCount();

    @Delete("DELETE FROM wvp_ai_inspection_result WHERE task_id IN (" +
            "SELECT t.id FROM wvp_ai_inspection_task t JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id " +
            "WHERE p.name LIKE 'CODEX-TEST-%')")
    int deleteTestResults();

    @Delete("DELETE FROM wvp_system_message WHERE business_type='DEVICE_ALARM' AND business_id IN (" +
            "SELECT r.alarm_id FROM wvp_ai_inspection_result r JOIN wvp_ai_inspection_task t ON r.task_id=t.id " +
            "JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id WHERE p.name LIKE 'CODEX-TEST-%' AND r.alarm_id IS NOT NULL)")
    int deleteTestMessages();

    @Delete("DELETE FROM wvp_device_alarm WHERE id IN (" +
            "SELECT r.alarm_id FROM wvp_ai_inspection_result r JOIN wvp_ai_inspection_task t ON r.task_id=t.id " +
            "JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id WHERE p.name LIKE 'CODEX-TEST-%' AND r.alarm_id IS NOT NULL)")
    int deleteTestAlarms();

    @Delete("DELETE FROM wvp_ai_work_order WHERE result_id IN (" +
            "SELECT r.id FROM wvp_ai_inspection_result r JOIN wvp_ai_inspection_task t ON r.task_id=t.id " +
            "JOIN wvp_ai_inspection_plan p ON t.plan_id=p.id WHERE p.name LIKE 'CODEX-TEST-%')")
    int deleteTestWorkOrders();

    @Delete("DELETE FROM wvp_ai_channel_health WHERE device_id LIKE 'CODEX-TEST-%' OR channel_id LIKE 'CODEX-TEST-%'")
    int deleteTestChannelHealth();

    @Delete("DELETE FROM wvp_ai_inspection_task WHERE plan_id IN (" +
            "SELECT id FROM wvp_ai_inspection_plan WHERE name LIKE 'CODEX-TEST-%')")
    int deleteTestTasks();

    @Delete("DELETE FROM wvp_ai_rule WHERE name LIKE 'CODEX-TEST-%'")
    int deleteTestRules();

    @Delete("DELETE FROM wvp_ai_model WHERE name LIKE 'CODEX-TEST-%'")
    int deleteTestModels();

    @Delete("DELETE FROM wvp_ai_inspection_plan WHERE name LIKE 'CODEX-TEST-%'")
    int deleteTestPlans();

    @Insert("INSERT INTO wvp_ai_channel_health(device_id,channel_id,online,stream_available,first_frame_millis," +
            "video_quality_score,recording_complete,health_score,health_status,diagnostic,snapshot_url,check_time) " +
            "VALUES(#{deviceId},#{channelId},#{online},#{streamAvailable},#{firstFrameMillis},#{videoQualityScore}," +
            "#{recordingComplete},#{healthScore},#{healthStatus},#{diagnostic},#{snapshotUrl},#{checkTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertChannelHealth(ChannelHealth health);

    @Select("SELECT h.* FROM wvp_ai_channel_health h JOIN (" +
            "SELECT channel_id,MAX(id) id FROM wvp_ai_channel_health GROUP BY channel_id" +
            ") latest ON h.id=latest.id ORDER BY h.health_score,h.id DESC")
    List<ChannelHealth> latestChannelHealth();

    @Insert("INSERT INTO wvp_ai_work_order(result_id,title,priority,status,due_time,create_time,update_time) " +
            "VALUES(#{resultId},#{title},#{priority},#{status},#{dueTime},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWorkOrder(InspectionWorkOrder order);

    @Select("SELECT * FROM wvp_ai_work_order ORDER BY CASE status WHEN 'OPEN' THEN 1 WHEN 'PROCESSING' THEN 2 " +
            "WHEN 'RESOLVED' THEN 3 ELSE 4 END,due_time,id DESC")
    List<InspectionWorkOrder> workOrders();

    @Select("SELECT * FROM wvp_ai_work_order WHERE id=#{id}")
    InspectionWorkOrder workOrder(Long id);

    @Update("UPDATE wvp_ai_work_order SET status='PROCESSING',assignee_id=#{userId},accepted_at=#{now},update_time=#{now} " +
            "WHERE id=#{id} AND status='OPEN'")
    int acceptWorkOrder(@Param("id") Long id, @Param("userId") Integer userId, @Param("now") String now);

    @Update("UPDATE wvp_ai_work_order SET status='RESOLVED',resolution=#{resolution},resolved_at=#{now},update_time=#{now} " +
            "WHERE id=#{id} AND assignee_id=#{userId} AND status='PROCESSING'")
    int resolveWorkOrder(@Param("id") Long id, @Param("userId") Integer userId,
                         @Param("resolution") String resolution, @Param("now") String now);

    @Update("UPDATE wvp_ai_work_order SET status=#{status},verified_at=#{now},update_time=#{now} WHERE id=#{id} AND status='RESOLVED'")
    int verifyWorkOrder(@Param("id") Long id, @Param("status") String status, @Param("now") String now);

    @Select("SELECT aggregation_key,root_cause,SUM(occurrence_count) event_count,COUNT(DISTINCT channel_id) affected_channels," +
            "MAX(priority) priority,MAX(create_time) latest_time FROM wvp_ai_inspection_result " +
            "WHERE workflow_status!='CLOSED' AND aggregation_key IS NOT NULL GROUP BY aggregation_key,root_cause ORDER BY latest_time DESC")
    List<IncidentGroup> incidentGroups();

    @Update("UPDATE wvp_ai_inspection_result SET workflow_status='CLOSED',recovered_at=#{now},handled_at=#{now} " +
            "WHERE aggregation_key=#{aggregationKey} AND workflow_status!='CLOSED'")
    int recoverIncident(@Param("aggregationKey") String aggregationKey, @Param("now") String now);

    @Select("SELECT m.id model_id,m.name model_name,m.version model_version,COUNT(r.id) total_count," +
            "SUM(CASE WHEN r.status='CONFIRMED' THEN 1 ELSE 0 END) confirmed_count," +
            "SUM(CASE WHEN r.status='FALSE_POSITIVE' THEN 1 ELSE 0 END) false_positive_count " +
            "FROM wvp_ai_model m LEFT JOIN wvp_ai_inspection_result r ON r.model_id=m.id " +
            "GROUP BY m.id,m.name,m.version ORDER BY m.id DESC")
    List<ModelQuality> modelQuality();

    @Select("SELECT * FROM wvp_ai_scene_template ORDER BY id DESC")
    List<SceneTemplate> sceneTemplates();

    @Select("SELECT * FROM wvp_ai_scene_template WHERE id=#{id}")
    SceneTemplate sceneTemplate(Integer id);

    @Insert("INSERT INTO wvp_ai_scene_template(code,name,description,status,version,create_time,update_time) " +
            "VALUES(#{code},#{name},#{description},#{status},#{version},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSceneTemplate(SceneTemplate template);

    @Select("SELECT * FROM wvp_ai_scene_region WHERE template_id=#{templateId} ORDER BY id")
    List<SceneRegion> sceneRegions(Integer templateId);

    @Insert("INSERT INTO wvp_ai_scene_region(template_id,name,region_type,polygon_points,excluded_points,channel_ids," +
            "algorithm_codes,active_days,start_time,end_time,enabled) VALUES(#{templateId},#{name},#{regionType}," +
            "#{polygonPoints},#{excludedPoints},#{channelIds},#{algorithmCodes},#{activeDays},#{startTime},#{endTime},#{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSceneRegion(SceneRegion region);
}
