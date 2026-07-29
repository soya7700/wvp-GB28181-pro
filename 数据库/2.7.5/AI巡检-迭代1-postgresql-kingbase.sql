ALTER TABLE wvp_ai_inspection_task
    ADD COLUMN IF NOT EXISTS retry_count integer NOT NULL DEFAULT 0;

ALTER TABLE wvp_ai_inspection_result
    ADD COLUMN IF NOT EXISTS callback_id varchar(100);

UPDATE wvp_ai_inspection_result
SET callback_id = 'legacy-' || id
WHERE callback_id IS NULL;

ALTER TABLE wvp_ai_inspection_result
    ALTER COLUMN callback_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_result_callback
    ON wvp_ai_inspection_result(callback_id);
