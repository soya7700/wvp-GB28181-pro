CREATE TABLE IF NOT EXISTS wvp_ai_algorithm (
 id int NOT NULL AUTO_INCREMENT, code varchar(50) NOT NULL, name varchar(100) NOT NULL,
 category varchar(30) NOT NULL, min_duration_seconds int NOT NULL, cooldown_seconds int NOT NULL,
 confidence_threshold decimal(6,5) NOT NULL, risk_level varchar(20) NOT NULL,
 enabled tinyint(1) NOT NULL DEFAULT 1, PRIMARY KEY(id), UNIQUE KEY uk_ai_algorithm_code(code)
);
CREATE TABLE IF NOT EXISTS wvp_ai_algorithm_event (
 id bigint NOT NULL AUTO_INCREMENT, event_uid varchar(100) NOT NULL, template_id int, region_id int,
 algorithm_code varchar(50) NOT NULL, algorithm_version varchar(50), device_id varchar(50),
 channel_id varchar(50) NOT NULL, target_id varchar(100), confidence decimal(6,5),
 start_time varchar(50), end_time varchar(50), duration_seconds int NOT NULL DEFAULT 0,
 state varchar(20) NOT NULL, evidence_url varchar(1000), clip_url varchar(1000),
 dedup_key varchar(250) NOT NULL, create_time varchar(50) NOT NULL, PRIMARY KEY(id),
 UNIQUE KEY uk_ai_algorithm_event_uid(event_uid), INDEX idx_ai_algorithm_event_state(state,create_time),
 INDEX idx_ai_algorithm_event_dedup(dedup_key,state)
);
