CREATE TABLE IF NOT EXISTS wvp_store_visit_task (
 id bigserial PRIMARY KEY,task_code varchar(50) NOT NULL UNIQUE,title varchar(200) NOT NULL,
 store_id varchar(50) NOT NULL,store_name varchar(100) NOT NULL,store_longitude decimal(11,7),store_latitude decimal(10,7),
 assignee_id integer NOT NULL,recorder_id bigint,planned_start_time varchar(50) NOT NULL,planned_end_time varchar(50) NOT NULL,
 status varchar(20) NOT NULL,checked_in_at varchar(50),checked_out_at varchar(50),checkin_distance_meters decimal(10,2),
 create_time varchar(50) NOT NULL,update_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_store_visit_assignee_status ON wvp_store_visit_task(assignee_id,status);
CREATE INDEX IF NOT EXISTS idx_store_visit_store_time ON wvp_store_visit_task(store_id,planned_start_time);
CREATE TABLE IF NOT EXISTS wvp_store_visit_check_result (
 id bigserial PRIMARY KEY,task_id bigint NOT NULL,submission_id varchar(100) NOT NULL UNIQUE,item_code varchar(50) NOT NULL,
 item_name varchar(200),item_group varchar(50),result varchar(20) NOT NULL,severity varchar(20),note varchar(1000),
 evidence_urls text,evidence_required boolean NOT NULL DEFAULT false,submitted_by integer NOT NULL,submitted_at varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_visit_check_task ON wvp_store_visit_check_result(task_id,id);
