ALTER TABLE wvp_device_alarm ADD COLUMN handling_status varchar(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE wvp_device_alarm ADD COLUMN handled_by int NULL;
ALTER TABLE wvp_device_alarm ADD COLUMN handled_at varchar(50) NULL;
ALTER TABLE wvp_device_alarm ADD COLUMN handling_note varchar(500) NULL;
CREATE INDEX idx_wvp_device_alarm_status ON wvp_device_alarm(handling_status);

CREATE TABLE IF NOT EXISTS wvp_system_message (
    id serial PRIMARY KEY,
    user_id int NOT NULL,
    type varchar(30) NOT NULL,
    title varchar(255) NOT NULL,
    content varchar(1000),
    level varchar(20),
    business_type varchar(30),
    business_id int,
    read_flag boolean NOT NULL DEFAULT false,
    read_time varchar(50),
    create_time varchar(50) NOT NULL
);
CREATE INDEX idx_wvp_message_user_read ON wvp_system_message(user_id, read_flag);
CREATE INDEX idx_wvp_message_business ON wvp_system_message(business_type, business_id);
