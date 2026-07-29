CREATE TABLE IF NOT EXISTS wvp_store_visit_task (
 id bigint NOT NULL AUTO_INCREMENT,task_code varchar(50) NOT NULL,title varchar(200) NOT NULL,
 store_id varchar(50) NOT NULL,store_name varchar(100) NOT NULL,store_longitude decimal(11,7),store_latitude decimal(10,7),
 assignee_id int NOT NULL,recorder_id bigint,planned_start_time varchar(50) NOT NULL,planned_end_time varchar(50) NOT NULL,
 status varchar(20) NOT NULL,checked_in_at varchar(50),checked_out_at varchar(50),checkin_distance_meters decimal(10,2),
 create_time varchar(50) NOT NULL,update_time varchar(50) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_store_visit_code(task_code),
 INDEX idx_store_visit_assignee_status(assignee_id,status),INDEX idx_store_visit_store_time(store_id,planned_start_time)
);
CREATE TABLE IF NOT EXISTS wvp_store_visit_check_result (
 id bigint NOT NULL AUTO_INCREMENT,task_id bigint NOT NULL,submission_id varchar(100) NOT NULL,item_code varchar(50) NOT NULL,
 item_name varchar(200),item_group varchar(50),result varchar(20) NOT NULL,severity varchar(20),note varchar(1000),
 evidence_urls text,evidence_required tinyint(1) NOT NULL DEFAULT 0,submitted_by int NOT NULL,submitted_at varchar(50) NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_visit_check_submission(submission_id),INDEX idx_visit_check_task(task_id,id)
);
