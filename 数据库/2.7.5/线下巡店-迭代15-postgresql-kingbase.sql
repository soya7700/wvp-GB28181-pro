CREATE TABLE IF NOT EXISTS wvp_store_visit_media (
 id bigserial PRIMARY KEY,task_id bigint NOT NULL,recorder_id bigint,upload_id varchar(100) NOT NULL UNIQUE,
 media_type varchar(20) NOT NULL,file_name varchar(255) NOT NULL,file_size bigint NOT NULL,checksum varchar(128),
 storage_url varchar(1000),thumbnail_url varchar(1000),status varchar(20) NOT NULL,uploaded_bytes bigint NOT NULL DEFAULT 0,
 captured_at varchar(50),uploaded_by integer,create_time varchar(50) NOT NULL,update_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_visit_media_task ON wvp_store_visit_media(task_id,id);
CREATE INDEX IF NOT EXISTS idx_visit_media_status ON wvp_store_visit_media(status,update_time);
CREATE TABLE IF NOT EXISTS wvp_stream_lease (
 id bigserial PRIMARY KEY,tenant_id varchar(50) NOT NULL,recorder_id bigint NOT NULL,
 business_type varchar(30) NOT NULL,lease_token varchar(100) NOT NULL UNIQUE,status varchar(20) NOT NULL,
 expires_at varchar(50) NOT NULL,create_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_stream_lease_quota ON wvp_stream_lease(tenant_id,status,expires_at);
