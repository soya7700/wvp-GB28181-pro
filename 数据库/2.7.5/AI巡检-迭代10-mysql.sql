ALTER TABLE wvp_ai_algorithm_event ADD COLUMN recovered_at varchar(50) NULL;
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN suppressed_until varchar(50) NULL;
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN occurrence_count int NOT NULL DEFAULT 1;
CREATE TABLE IF NOT EXISTS wvp_ai_maintenance_window (
  id bigint NOT NULL AUTO_INCREMENT, scope_type varchar(20) NOT NULL, scope_id varchar(100) NOT NULL,
  start_time varchar(50) NOT NULL, end_time varchar(50) NOT NULL, reason varchar(500),
  enabled tinyint(1) NOT NULL DEFAULT 1, create_time varchar(50) NOT NULL, PRIMARY KEY(id),
  INDEX idx_ai_maintenance_active(enabled,start_time,end_time), INDEX idx_ai_maintenance_scope(scope_type,scope_id)
);
