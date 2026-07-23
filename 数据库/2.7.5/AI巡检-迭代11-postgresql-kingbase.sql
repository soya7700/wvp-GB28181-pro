ALTER TABLE wvp_ai_algorithm_event ADD COLUMN IF NOT EXISTS review_status varchar(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN IF NOT EXISTS reviewed_by integer;
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN IF NOT EXISTS reviewed_at varchar(50);
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN IF NOT EXISTS review_note varchar(500);
CREATE INDEX IF NOT EXISTS idx_ai_algorithm_event_review ON wvp_ai_algorithm_event(review_status,reviewed_at);
