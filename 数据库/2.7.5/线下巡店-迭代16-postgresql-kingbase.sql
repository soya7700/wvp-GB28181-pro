CREATE TABLE IF NOT EXISTS wvp_store_visit_rectification (
 id bigserial PRIMARY KEY,task_id bigint NOT NULL,check_result_id bigint NOT NULL UNIQUE,store_id varchar(50) NOT NULL,
 store_name varchar(100) NOT NULL,title varchar(200) NOT NULL,severity varchar(20) NOT NULL,assignee_id integer NOT NULL,
 status varchar(20) NOT NULL,due_time varchar(50) NOT NULL,resolution varchar(1000),evidence_urls text,
 submitted_at varchar(50),reviewed_at varchar(50),reviewed_by integer,review_note varchar(1000),
 create_time varchar(50) NOT NULL,update_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_visit_rectification_status_due ON wvp_store_visit_rectification(status,due_time);
CREATE INDEX IF NOT EXISTS idx_visit_rectification_assignee ON wvp_store_visit_rectification(assignee_id,status);
