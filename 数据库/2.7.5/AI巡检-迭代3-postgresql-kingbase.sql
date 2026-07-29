ALTER TABLE wvp_ai_inspection_result
    ADD COLUMN IF NOT EXISTS workflow_status varchar(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN IF NOT EXISTS priority varchar(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS assignee_id integer,
    ADD COLUMN IF NOT EXISTS occurrence_count integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS handling_note varchar(500),
    ADD COLUMN IF NOT EXISTS handled_at varchar(50);

CREATE INDEX IF NOT EXISTS idx_ai_result_workflow
    ON wvp_ai_inspection_result(workflow_status, assignee_id, priority);
