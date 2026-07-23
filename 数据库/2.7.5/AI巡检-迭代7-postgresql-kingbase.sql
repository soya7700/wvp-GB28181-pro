ALTER TABLE wvp_ai_model
  ADD COLUMN IF NOT EXISTS traffic_percent integer NOT NULL DEFAULT 0;

ALTER TABLE wvp_ai_inspection_result
  ADD COLUMN IF NOT EXISTS model_id integer,
  ADD COLUMN IF NOT EXISTS rule_id integer;

CREATE INDEX IF NOT EXISTS idx_ai_result_model_status
  ON wvp_ai_inspection_result(model_id, status, create_time);
