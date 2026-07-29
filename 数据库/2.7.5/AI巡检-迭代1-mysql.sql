ALTER TABLE wvp_ai_inspection_task
    ADD COLUMN retry_count int NOT NULL DEFAULT 0;

ALTER TABLE wvp_ai_inspection_result
    ADD COLUMN callback_id varchar(100) NULL;

UPDATE wvp_ai_inspection_result
SET callback_id = CONCAT('legacy-', id)
WHERE callback_id IS NULL;

ALTER TABLE wvp_ai_inspection_result
    MODIFY COLUMN callback_id varchar(100) NOT NULL;

CREATE UNIQUE INDEX uk_ai_result_callback
    ON wvp_ai_inspection_result(callback_id);
