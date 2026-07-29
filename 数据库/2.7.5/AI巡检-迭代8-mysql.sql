CREATE TABLE IF NOT EXISTS wvp_ai_scene_template (
  id int NOT NULL AUTO_INCREMENT, code varchar(50) NOT NULL, name varchar(100) NOT NULL,
  description varchar(500), status varchar(20) NOT NULL, version int NOT NULL DEFAULT 1,
  create_time varchar(50) NOT NULL, update_time varchar(50) NOT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_ai_scene_template_code(code)
);
CREATE TABLE IF NOT EXISTS wvp_ai_scene_region (
  id int NOT NULL AUTO_INCREMENT, template_id int NOT NULL, name varchar(100) NOT NULL,
  region_type varchar(50) NOT NULL, polygon_points text, excluded_points text, channel_ids text,
  algorithm_codes varchar(1000), active_days varchar(30) NOT NULL DEFAULT '1,2,3,4,5,6,7',
  start_time varchar(5) NOT NULL DEFAULT '00:00', end_time varchar(5) NOT NULL DEFAULT '23:59',
  enabled tinyint(1) NOT NULL DEFAULT 1, PRIMARY KEY(id), INDEX idx_ai_scene_region_template(template_id)
);
