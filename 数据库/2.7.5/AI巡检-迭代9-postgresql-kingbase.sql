CREATE TABLE IF NOT EXISTS wvp_ai_algorithm (
 id serial PRIMARY KEY, code varchar(50) NOT NULL UNIQUE, name varchar(100) NOT NULL,
 category varchar(30) NOT NULL, min_duration_seconds integer NOT NULL, cooldown_seconds integer NOT NULL,
 confidence_threshold decimal(6,5) NOT NULL, risk_level varchar(20) NOT NULL, enabled boolean NOT NULL DEFAULT true
);
CREATE TABLE IF NOT EXISTS wvp_ai_algorithm_event (
 id bigserial PRIMARY KEY, event_uid varchar(100) NOT NULL UNIQUE, template_id integer, region_id integer,
 algorithm_code varchar(50) NOT NULL, algorithm_version varchar(50), device_id varchar(50),
 channel_id varchar(50) NOT NULL, target_id varchar(100), confidence decimal(6,5),
 start_time varchar(50), end_time varchar(50), duration_seconds integer NOT NULL DEFAULT 0,
 state varchar(20) NOT NULL, evidence_url varchar(1000), clip_url varchar(1000),
 dedup_key varchar(250) NOT NULL, create_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_algorithm_event_state ON wvp_ai_algorithm_event(state,create_time);
CREATE INDEX IF NOT EXISTS idx_ai_algorithm_event_dedup ON wvp_ai_algorithm_event(dedup_key,state);
