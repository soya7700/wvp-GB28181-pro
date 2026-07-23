ALTER TABLE wvp_ai_inspection_plan
    ADD COLUMN schedule_days varchar(30) NOT NULL DEFAULT '1,2,3,4,5,6,7',
    ADD COLUMN start_time varchar(5) NOT NULL DEFAULT '00:00',
    ADD COLUMN end_time varchar(5) NOT NULL DEFAULT '23:59';
