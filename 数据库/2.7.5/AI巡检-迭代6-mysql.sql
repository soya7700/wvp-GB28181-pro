ALTER TABLE wvp_ai_inspection_result
  ADD COLUMN aggregation_key varchar(150),
  ADD COLUMN root_cause varchar(50),
  ADD COLUMN recovered_at varchar(50);

CREATE INDEX idx_ai_result_aggregation
  ON wvp_ai_inspection_result(aggregation_key, workflow_status, create_time);
