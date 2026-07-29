ALTER TABLE wvp_ai_model
  ADD COLUMN traffic_percent int NOT NULL DEFAULT 0;

ALTER TABLE wvp_ai_inspection_result
  ADD COLUMN model_id int,
  ADD COLUMN rule_id int;

CREATE INDEX idx_ai_result_model_status
  ON wvp_ai_inspection_result(model_id, status, create_time);
