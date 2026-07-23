CREATE TABLE IF NOT EXISTS wvp_ai_inspection_plan (
  id serial PRIMARY KEY,
  name varchar(100) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  interval_minutes integer NOT NULL DEFAULT 30,
  detection_types varchar(255) NOT NULL,
  channel_ids text,
  schedule_days varchar(30) NOT NULL DEFAULT '1,2,3,4,5,6,7',
  start_time varchar(5) NOT NULL DEFAULT '00:00',
  end_time varchar(5) NOT NULL DEFAULT '23:59',
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
  error_message varchar(500),
  retry_count integer NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ai_task_plan_time ON wvp_ai_inspection_task(plan_id, start_time);

CREATE TABLE IF NOT EXISTS wvp_ai_inspection_result (
  id bigserial PRIMARY KEY,
  task_id bigint NOT NULL,
  callback_id varchar(100) NOT NULL,
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
  ,workflow_status varchar(20) NOT NULL DEFAULT 'NEW'
  ,priority varchar(20) NOT NULL DEFAULT 'NORMAL'
  ,assignee_id integer
  ,occurrence_count integer NOT NULL DEFAULT 1
  ,handling_note varchar(500)
  ,handled_at varchar(50)
);
CREATE INDEX IF NOT EXISTS idx_ai_result_task ON wvp_ai_inspection_result(task_id);
CREATE INDEX IF NOT EXISTS idx_ai_result_status_time ON wvp_ai_inspection_result(status, create_time);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_result_callback ON wvp_ai_inspection_result(callback_id);
CREATE INDEX IF NOT EXISTS idx_ai_result_workflow ON wvp_ai_inspection_result(workflow_status, assignee_id, priority);

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

CREATE TABLE IF NOT EXISTS wvp_ai_channel_health (
  id bigserial PRIMARY KEY,
  device_id varchar(50),
  channel_id varchar(50) NOT NULL,
  online boolean NOT NULL DEFAULT false,
  stream_available boolean NOT NULL DEFAULT false,
  first_frame_millis integer,
  video_quality_score integer NOT NULL DEFAULT 100,
  recording_complete boolean NOT NULL DEFAULT false,
  health_score integer NOT NULL,
  health_status varchar(20) NOT NULL,
  diagnostic varchar(500),
  snapshot_url varchar(1000),
  check_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_health_channel_time ON wvp_ai_channel_health(channel_id, check_time);
CREATE INDEX IF NOT EXISTS idx_ai_health_status_score ON wvp_ai_channel_health(health_status, health_score);
