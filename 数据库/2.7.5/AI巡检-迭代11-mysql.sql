ALTER TABLE wvp_ai_algorithm_event ADD COLUMN review_status varchar(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN reviewed_by int NULL;
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN reviewed_at varchar(50) NULL;
ALTER TABLE wvp_ai_algorithm_event ADD COLUMN review_note varchar(500) NULL;
CREATE INDEX idx_ai_algorithm_event_review ON wvp_ai_algorithm_event(review_status,reviewed_at);
