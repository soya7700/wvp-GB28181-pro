CREATE TABLE IF NOT EXISTS wvp_ai_inspection_plan (
  id serial PRIMARY KEY,
  name varchar(100) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  interval_minutes integer NOT NULL DEFAULT 30,
  detection_types varchar(255) NOT NULL,
  channel_ids text,
  create_time varchar(50) NOT NULL,
  update_time varchar(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS wvp_ai_inspection_task (
  id bigserial PRIMARY KEY,
  plan_id integer NOT NULL,
  status varchar(20) NOT NULL,
  channel_total integer NOT NULL DEFAULT 0,
  success_count integer NOT NULL DEFAULT 0,
  abnormal_count integer NOT NULL DEFAULT 0,
  start_time varchar(50),
  end_time varchar(50),
  error_message varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_ai_task_plan_time ON wvp_ai_inspection_task(plan_id, start_time);

CREATE TABLE IF NOT EXISTS wvp_ai_inspection_result (
  id bigserial PRIMARY KEY,
  task_id bigint NOT NULL,
  device_id varchar(50),
  channel_id varchar(50) NOT NULL,
  detection_type varchar(30) NOT NULL,
  confidence decimal(6,5),
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  evidence_url varchar(1000),
  marked_url varchar(1000),
  create_time varchar(50) NOT NULL,
  review_note varchar(500)
  ,reviewed_by integer
  ,reviewed_at varchar(50)
  ,alarm_id integer
);
CREATE INDEX IF NOT EXISTS idx_ai_result_task ON wvp_ai_inspection_result(task_id);
CREATE INDEX IF NOT EXISTS idx_ai_result_status_time ON wvp_ai_inspection_result(status, create_time);

CREATE TABLE IF NOT EXISTS wvp_ai_model (
  id serial PRIMARY KEY,
  name varchar(100) NOT NULL,
  version varchar(50) NOT NULL,
  capabilities varchar(500) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'INACTIVE',
  service_endpoint varchar(500),
  create_time varchar(50) NOT NULL,
  UNIQUE(name, version)
);

CREATE TABLE IF NOT EXISTS wvp_ai_rule (
  id serial PRIMARY KEY,
  name varchar(100) NOT NULL,
  plan_id integer,
  detection_type varchar(30) NOT NULL,
  confidence_threshold decimal(6,5) NOT NULL DEFAULT 0.8,
  region_points text,
  enabled boolean NOT NULL DEFAULT true,
  create_time varchar(50) NOT NULL,
  update_time varchar(50) NOT NULL
);
