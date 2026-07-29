CREATE TABLE IF NOT EXISTS wvp_store_visit_media (
 id bigint NOT NULL AUTO_INCREMENT,task_id bigint NOT NULL,recorder_id bigint,upload_id varchar(100) NOT NULL,
 media_type varchar(20) NOT NULL,file_name varchar(255) NOT NULL,file_size bigint NOT NULL,checksum varchar(128),
 storage_url varchar(1000),thumbnail_url varchar(1000),status varchar(20) NOT NULL,uploaded_bytes bigint NOT NULL DEFAULT 0,
 captured_at varchar(50),uploaded_by int,create_time varchar(50) NOT NULL,update_time varchar(50) NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_visit_media_upload(upload_id),INDEX idx_visit_media_task(task_id,id),
 INDEX idx_visit_media_status(status,update_time)
);
CREATE TABLE IF NOT EXISTS wvp_stream_lease (
 id bigint NOT NULL AUTO_INCREMENT,tenant_id varchar(50) NOT NULL,recorder_id bigint NOT NULL,
 business_type varchar(30) NOT NULL,lease_token varchar(100) NOT NULL,status varchar(20) NOT NULL,
 expires_at varchar(50) NOT NULL,create_time varchar(50) NOT NULL,PRIMARY KEY(id),
 UNIQUE KEY uk_stream_lease_token(lease_token),INDEX idx_stream_lease_quota(tenant_id,status,expires_at)
);
