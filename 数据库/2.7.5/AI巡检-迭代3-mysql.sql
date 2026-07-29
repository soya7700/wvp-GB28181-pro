ALTER TABLE wvp_ai_inspection_result
    ADD COLUMN workflow_status varchar(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN priority varchar(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN assignee_id int NULL,
    ADD COLUMN occurrence_count int NOT NULL DEFAULT 1,
    ADD COLUMN handling_note varchar(500) NULL,
    ADD COLUMN handled_at varchar(50) NULL;

CREATE INDEX idx_ai_result_workflow
    ON wvp_ai_inspection_result(workflow_status, assignee_id, priority);
