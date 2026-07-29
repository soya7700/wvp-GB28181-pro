CREATE TABLE IF NOT EXISTS wvp_ai_scene_template (
  id serial PRIMARY KEY, code varchar(50) NOT NULL UNIQUE, name varchar(100) NOT NULL,
  description varchar(500), status varchar(20) NOT NULL, version integer NOT NULL DEFAULT 1,
  create_time varchar(50) NOT NULL, update_time varchar(50) NOT NULL
);
CREATE TABLE IF NOT EXISTS wvp_ai_scene_region (
  id serial PRIMARY KEY, template_id integer NOT NULL, name varchar(100) NOT NULL,
  region_type varchar(50) NOT NULL, polygon_points text, excluded_points text, channel_ids text,
  algorithm_codes varchar(1000), active_days varchar(30) NOT NULL DEFAULT '1,2,3,4,5,6,7',
  start_time varchar(5) NOT NULL DEFAULT '00:00', end_time varchar(5) NOT NULL DEFAULT '23:59',
  enabled boolean NOT NULL DEFAULT true
);
CREATE INDEX IF NOT EXISTS idx_ai_scene_region_template ON wvp_ai_scene_region(template_id);
