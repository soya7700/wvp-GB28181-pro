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
