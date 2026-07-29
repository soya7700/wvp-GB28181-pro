CREATE TABLE IF NOT EXISTS wvp_ai_channel_health (
  id bigint NOT NULL AUTO_INCREMENT,
  device_id varchar(50),
  channel_id varchar(50) NOT NULL,
  online tinyint(1) NOT NULL DEFAULT 0,
  stream_available tinyint(1) NOT NULL DEFAULT 0,
  first_frame_millis int,
  video_quality_score int NOT NULL DEFAULT 100,
  recording_complete tinyint(1) NOT NULL DEFAULT 0,
  health_score int NOT NULL,
  health_status varchar(20) NOT NULL,
  diagnostic varchar(500),
  snapshot_url varchar(1000),
  check_time varchar(50) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_ai_health_channel_time(channel_id, check_time),
  INDEX idx_ai_health_status_score(health_status, health_score)
);
