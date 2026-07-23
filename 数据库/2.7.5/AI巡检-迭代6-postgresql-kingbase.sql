ALTER TABLE wvp_ai_inspection_result
  ADD COLUMN IF NOT EXISTS aggregation_key varchar(150),
  ADD COLUMN IF NOT EXISTS root_cause varchar(50),
  ADD COLUMN IF NOT EXISTS recovered_at varchar(50);

CREATE INDEX IF NOT EXISTS idx_ai_result_aggregation
  ON wvp_ai_inspection_result(aggregation_key, workflow_status, create_time);
