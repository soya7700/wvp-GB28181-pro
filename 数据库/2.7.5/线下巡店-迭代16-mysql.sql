CREATE TABLE IF NOT EXISTS wvp_store_visit_rectification (
 id bigint NOT NULL AUTO_INCREMENT,task_id bigint NOT NULL,check_result_id bigint NOT NULL,store_id varchar(50) NOT NULL,
 store_name varchar(100) NOT NULL,title varchar(200) NOT NULL,severity varchar(20) NOT NULL,assignee_id int NOT NULL,
 status varchar(20) NOT NULL,due_time varchar(50) NOT NULL,resolution varchar(1000),evidence_urls text,
 submitted_at varchar(50),reviewed_at varchar(50),reviewed_by int,review_note varchar(1000),
 create_time varchar(50) NOT NULL,update_time varchar(50) NOT NULL,PRIMARY KEY(id),
 UNIQUE KEY uk_visit_rectification_result(check_result_id),INDEX idx_visit_rectification_status_due(status,due_time),
 INDEX idx_visit_rectification_assignee(assignee_id,status)
);
