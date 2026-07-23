CREATE TABLE IF NOT EXISTS wvp_mobile_recorder (
  id bigserial PRIMARY KEY, device_code varchar(100) NOT NULL UNIQUE, name varchar(100) NOT NULL,
  vendor varchar(50), model varchar(100), protocol_type varchar(30) NOT NULL, sim_number varchar(50),
  organization_id varchar(50), assigned_user_id integer, status varchar(20) NOT NULL,
  battery_level integer, storage_percent integer, network_status varchar(20), capabilities varchar(500),
  last_online_time varchar(50), create_time varchar(50) NOT NULL, update_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_mobile_recorder_org_status ON wvp_mobile_recorder(organization_id,status);
CREATE TABLE IF NOT EXISTS wvp_mobile_recorder_location (
  id bigserial PRIMARY KEY, recorder_id bigint NOT NULL, longitude decimal(11,7) NOT NULL,
  latitude decimal(10,7) NOT NULL, coordinate_type varchar(20) NOT NULL, accuracy_meters integer,
  locate_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_recorder_location_time ON wvp_mobile_recorder_location(recorder_id,locate_time);
