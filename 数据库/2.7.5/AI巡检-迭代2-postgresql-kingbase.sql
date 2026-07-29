ALTER TABLE wvp_ai_inspection_plan
    ADD COLUMN IF NOT EXISTS schedule_days varchar(30) NOT NULL DEFAULT '1,2,3,4,5,6,7',
    ADD COLUMN IF NOT EXISTS start_time varchar(5) NOT NULL DEFAULT '00:00',
    ADD COLUMN IF NOT EXISTS end_time varchar(5) NOT NULL DEFAULT '23:59';
