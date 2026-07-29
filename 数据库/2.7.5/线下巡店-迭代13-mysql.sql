CREATE TABLE IF NOT EXISTS wvp_mobile_recorder (
  id bigint NOT NULL AUTO_INCREMENT, device_code varchar(100) NOT NULL, name varchar(100) NOT NULL,
  vendor varchar(50), model varchar(100), protocol_type varchar(30) NOT NULL, sim_number varchar(50),
  organization_id varchar(50), assigned_user_id int, status varchar(20) NOT NULL,
  battery_level int, storage_percent int, network_status varchar(20), capabilities varchar(500),
  last_online_time varchar(50), create_time varchar(50) NOT NULL, update_time varchar(50) NOT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_mobile_recorder_code(device_code),
  INDEX idx_mobile_recorder_org_status(organization_id,status)
);
CREATE TABLE IF NOT EXISTS wvp_mobile_recorder_location (
  id bigint NOT NULL AUTO_INCREMENT, recorder_id bigint NOT NULL, longitude decimal(11,7) NOT NULL,
  latitude decimal(10,7) NOT NULL, coordinate_type varchar(20) NOT NULL, accuracy_meters int,
  locate_time varchar(50) NOT NULL, PRIMARY KEY(id), INDEX idx_recorder_location_time(recorder_id,locate_time)
);
