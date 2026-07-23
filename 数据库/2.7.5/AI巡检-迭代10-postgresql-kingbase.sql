ALTER TABLE wvp_ai_algorithm_event ADD COLUMN IF NOT EXISTS recovered_at varchar(50);
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN IF NOT EXISTS suppressed_until varchar(50);
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN IF NOT EXISTS occurrence_count integer NOT NULL DEFAULT 1;
CREATE TABLE IF NOT EXISTS wvp_ai_maintenance_window (
  id bigserial PRIMARY KEY, scope_type varchar(20) NOT NULL, scope_id varchar(100) NOT NULL,
  start_time varchar(50) NOT NULL, end_time varchar(50) NOT NULL, reason varchar(500),
  enabled boolean NOT NULL DEFAULT true, create_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_maintenance_active ON wvp_ai_maintenance_window(enabled,start_time,end_time);
CREATE INDEX IF NOT EXISTS idx_ai_maintenance_scope ON wvp_ai_maintenance_window(scope_type,scope_id);
