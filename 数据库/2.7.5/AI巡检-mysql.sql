CREATE TABLE IF NOT EXISTS wvp_ai_inspection_plan (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  interval_minutes int NOT NULL DEFAULT 30,
  detection_types varchar(255) NOT NULL,
  channel_ids text,
  schedule_days varchar(30) NOT NULL DEFAULT '1,2,3,4,5,6,7',
  start_time varchar(5) NOT NULL DEFAULT '00:00',
  end_time varchar(5) NOT NULL DEFAULT '23:59',
  create_time varchar(50) NOT NULL,
  update_time varchar(50) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS wvp_ai_inspection_task (
  id bigint NOT NULL AUTO_INCREMENT,
  plan_id int NOT NULL,
  status varchar(20) NOT NULL,
  channel_total int NOT NULL DEFAULT 0,
  success_count int NOT NULL DEFAULT 0,
  abnormal_count int NOT NULL DEFAULT 0,
  start_time varchar(50),
  end_time varchar(50),
  error_message varchar(500),
  retry_count int NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_ai_task_plan_time(plan_id, start_time)
);

CREATE TABLE IF NOT EXISTS wvp_ai_inspection_result (
  id bigint NOT NULL AUTO_INCREMENT,
  callback_id varchar(100) NOT NULL,
  task_id bigint NOT NULL,
  device_id varchar(50),
  channel_id varchar(50) NOT NULL,
  detection_type varchar(30) NOT NULL,
  confidence decimal(6,5),
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  evidence_url varchar(1000),
  marked_url varchar(1000),
  create_time varchar(50) NOT NULL,
  review_note varchar(500),
  reviewed_by int,
  reviewed_at varchar(50),
  alarm_id int,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_result_callback(callback_id),
  INDEX idx_ai_result_task(task_id),
  INDEX idx_ai_result_status_time(status, create_time)
);

CREATE TABLE IF NOT EXISTS wvp_ai_model (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  version varchar(50) NOT NULL,
  capabilities varchar(500) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'INACTIVE',
  service_endpoint varchar(500),
  create_time varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_model_name_version(name, version)
);

CREATE TABLE IF NOT EXISTS wvp_ai_rule (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  plan_id int,
  detection_type varchar(30) NOT NULL,
  confidence_threshold decimal(6,5) NOT NULL DEFAULT 0.8,
  region_points text,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  create_time varchar(50) NOT NULL,
  update_time varchar(50) NOT NULL,
  PRIMARY KEY (id)
);
