CREATE TABLE IF NOT EXISTS wvp_ai_work_order (
  id bigint NOT NULL AUTO_INCREMENT,
  result_id bigint NOT NULL,
  title varchar(200) NOT NULL,
  priority varchar(20) NOT NULL,
  status varchar(20) NOT NULL,
  assignee_id int,
  due_time varchar(50) NOT NULL,
  accepted_at varchar(50),
  resolved_at varchar(50),
  verified_at varchar(50),
  resolution varchar(1000),
  create_time varchar(50) NOT NULL,
  update_time varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_work_order_result(result_id),
  INDEX idx_ai_work_order_status_due(status, due_time)
);
