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
import com.genersoft.iot.vmp.vmanager.inspection.bean.AlgorithmDefinition;
import com.genersoft.iot.vmp.vmanager.inspection.bean.AlgorithmEvent;
import com.genersoft.iot.vmp.vmanager.inspection.bean.MaintenanceWindow;
import com.genersoft.iot.vmp.vmanager.inspection.bean.SceneRiskSummary;
import com.genersoft.iot.vmp.vmanager.inspection.bean.MobileRecorder;
import com.genersoft.iot.vmp.vmanager.inspection.bean.RecorderLocation;
import com.genersoft.iot.vmp.vmanager.inspection.bean.StoreVisitTask;
import com.genersoft.iot.vmp.vmanager.inspection.bean.VisitChecklistResult;
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
            "'wvp_ai_channel_health','wvp_ai_work_order','wvp_ai_scene_template','wvp_ai_scene_region'," +
            "'wvp_ai_algorithm','wvp_ai_algorithm_event','wvp_ai_maintenance_window')")
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

    @Select("SELECT * FROM wvp_ai_algorithm ORDER BY category,code")
    List<AlgorithmDefinition> algorithms();

    @Select("SELECT * FROM wvp_ai_algorithm WHERE code=#{code} AND enabled=true")
    AlgorithmDefinition algorithm(String code);

    @Insert("INSERT INTO wvp_ai_algorithm(code,name,category,min_duration_seconds,cooldown_seconds,confidence_threshold,risk_level,enabled) " +
            "VALUES(#{code},#{name},#{category},#{minDurationSeconds},#{cooldownSeconds},#{confidenceThreshold},#{riskLevel},#{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAlgorithm(AlgorithmDefinition algorithm);

    @Select("SELECT * FROM wvp_ai_algorithm_event WHERE dedup_key=#{dedupKey} AND state IN ('OBSERVING','OPEN') ORDER BY id DESC LIMIT 1")
    AlgorithmEvent openAlgorithmEvent(String dedupKey);

    @Insert("INSERT INTO wvp_ai_algorithm_event(event_uid,template_id,region_id,algorithm_code,algorithm_version,device_id," +
            "channel_id,target_id,confidence,start_time,end_time,duration_seconds,state,evidence_url,clip_url,dedup_key,suppressed_until,occurrence_count,create_time) " +
            "VALUES(#{eventUid},#{templateId},#{regionId},#{algorithmCode},#{algorithmVersion},#{deviceId},#{channelId}," +
            "#{targetId},#{confidence},#{startTime},#{endTime},#{durationSeconds},#{state},#{evidenceUrl},#{clipUrl},#{dedupKey},#{suppressedUntil},#{occurrenceCount},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAlgorithmEvent(AlgorithmEvent event);

    @Select("SELECT * FROM wvp_ai_algorithm_event ORDER BY id DESC LIMIT 200")
    List<AlgorithmEvent> algorithmEvents();

    @Select("SELECT * FROM wvp_ai_maintenance_window WHERE enabled=true AND start_time<=#{now} AND end_time>=#{now} " +
            "AND ((scope_type='CHANNEL' AND scope_id=#{channelId}) OR (scope_type='REGION' AND scope_id=#{regionId})) ORDER BY id DESC LIMIT 1")
    MaintenanceWindow activeMaintenanceWindow(@Param("channelId") String channelId, @Param("regionId") String regionId, @Param("now") String now);

    @Select("SELECT * FROM wvp_ai_maintenance_window ORDER BY start_time DESC")
    List<MaintenanceWindow> maintenanceWindows();

    @Insert("INSERT INTO wvp_ai_maintenance_window(scope_type,scope_id,start_time,end_time,reason,enabled,create_time) VALUES(#{scopeType},#{scopeId},#{startTime},#{endTime},#{reason},#{enabled},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMaintenanceWindow(MaintenanceWindow window);

    @Update("UPDATE wvp_ai_algorithm_event SET state='RECOVERED',recovered_at=#{now} WHERE id=#{id} AND state='OPEN'")
    int recoverAlgorithmEvent(@Param("id") Long id, @Param("now") String now);

    @Select("SELECT * FROM wvp_ai_algorithm_event WHERE id=#{id}")
    AlgorithmEvent algorithmEvent(Long id);

    @Update("UPDATE wvp_ai_algorithm_event SET review_status=#{status},reviewed_by=#{userId},reviewed_at=#{now}," +
            "review_note=#{note},state='CLOSED' WHERE id=#{id} AND review_status='PENDING'")
    int reviewAlgorithmEvent(@Param("id") Long id, @Param("status") String status, @Param("userId") Integer userId,
                             @Param("note") String note, @Param("now") String now);

    @Select("SELECT t.id template_id,t.name template_name,COUNT(e.id) event_count," +
            "SUM(CASE WHEN e.state='OPEN' THEN 1 ELSE 0 END) open_count," +
            "SUM(CASE WHEN a.risk_level IN ('HIGH','URGENT') THEN 1 ELSE 0 END) high_risk_count," +
            "SUM(CASE WHEN e.review_status='CONFIRMED' THEN 1 ELSE 0 END) confirmed_count," +
            "SUM(CASE WHEN e.review_status='FALSE_POSITIVE' THEN 1 ELSE 0 END) false_positive_count," +
            "COALESCE(SUM(CASE a.risk_level WHEN 'URGENT' THEN 10 WHEN 'HIGH' THEN 6 WHEN 'NORMAL' THEN 3 ELSE 1 END),0) risk_score " +
            "FROM wvp_ai_scene_template t LEFT JOIN wvp_ai_algorithm_event e ON e.template_id=t.id " +
            "LEFT JOIN wvp_ai_algorithm a ON a.code=e.algorithm_code GROUP BY t.id,t.name ORDER BY risk_score DESC")
    List<SceneRiskSummary> sceneRiskSummaries();

    @Select("SELECT * FROM wvp_mobile_recorder ORDER BY id DESC")
    List<MobileRecorder> mobileRecorders();

    @Select("SELECT * FROM wvp_mobile_recorder WHERE id=#{id}")
    MobileRecorder mobileRecorder(Long id);

    @Select("SELECT * FROM wvp_mobile_recorder WHERE device_code=#{deviceCode}")
    MobileRecorder mobileRecorderByCode(String deviceCode);

    @Insert("INSERT INTO wvp_mobile_recorder(device_code,name,vendor,model,protocol_type,sim_number,organization_id," +
            "assigned_user_id,status,battery_level,storage_percent,network_status,capabilities,last_online_time,create_time,update_time) " +
            "VALUES(#{deviceCode},#{name},#{vendor},#{model},#{protocolType},#{simNumber},#{organizationId},#{assignedUserId}," +
            "#{status},#{batteryLevel},#{storagePercent},#{networkStatus},#{capabilities},#{lastOnlineTime},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMobileRecorder(MobileRecorder recorder);

    @Update("UPDATE wvp_mobile_recorder SET assigned_user_id=#{userId},status=#{status},update_time=#{now} WHERE id=#{id}")
    int assignMobileRecorder(@Param("id") Long id, @Param("userId") Integer userId,
                             @Param("status") String status, @Param("now") String now);

    @Insert("INSERT INTO wvp_mobile_recorder_location(recorder_id,longitude,latitude,coordinate_type,accuracy_meters,locate_time) " +
            "VALUES(#{recorderId},#{longitude},#{latitude},#{coordinateType},#{accuracyMeters},#{locateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecorderLocation(RecorderLocation location);

    @Select("SELECT * FROM wvp_mobile_recorder_location WHERE recorder_id=#{recorderId} ORDER BY locate_time DESC LIMIT 200")
    List<RecorderLocation> recorderLocations(Long recorderId);

    @Select("SELECT * FROM wvp_store_visit_task ORDER BY id DESC")
    List<StoreVisitTask> storeVisitTasks();

    @Select("SELECT * FROM wvp_store_visit_task WHERE id=#{id}")
    StoreVisitTask storeVisitTask(Long id);

    @Insert("INSERT INTO wvp_store_visit_task(task_code,title,store_id,store_name,store_longitude,store_latitude,assignee_id," +
            "recorder_id,planned_start_time,planned_end_time,status,create_time,update_time) VALUES(#{taskCode},#{title}," +
            "#{storeId},#{storeName},#{storeLongitude},#{storeLatitude},#{assigneeId},#{recorderId},#{plannedStartTime}," +
            "#{plannedEndTime},#{status},#{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertStoreVisitTask(StoreVisitTask task);

    @Update("UPDATE wvp_store_visit_task SET status='IN_PROGRESS',checked_in_at=#{now},checkin_distance_meters=#{distance},update_time=#{now} WHERE id=#{id} AND status='PENDING'")
    int checkinStoreVisitTask(@Param("id") Long id, @Param("distance") Double distance, @Param("now") String now);

    @Update("UPDATE wvp_store_visit_task SET status='COMPLETED',checked_out_at=#{now},update_time=#{now} WHERE id=#{id} AND status='IN_PROGRESS'")
    int checkoutStoreVisitTask(@Param("id") Long id, @Param("now") String now);

    @Select("SELECT * FROM wvp_store_visit_check_result WHERE submission_id=#{submissionId} LIMIT 1")
    VisitChecklistResult checklistResultBySubmission(String submissionId);

    @Insert("INSERT INTO wvp_store_visit_check_result(task_id,submission_id,item_code,item_name,item_group,result,severity,note," +
            "evidence_urls,evidence_required,submitted_by,submitted_at) VALUES(#{taskId},#{submissionId},#{itemCode},#{itemName}," +
            "#{itemGroup},#{result},#{severity},#{note},#{evidenceUrls},#{evidenceRequired},#{submittedBy},#{submittedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertChecklistResult(VisitChecklistResult result);

    @Select("SELECT * FROM wvp_store_visit_check_result WHERE task_id=#{taskId} ORDER BY id")
    List<VisitChecklistResult> checklistResults(Long taskId);
}
