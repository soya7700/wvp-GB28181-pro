CREATE TABLE IF NOT EXISTS wvp_ai_work_order (
  id bigserial PRIMARY KEY,
  result_id bigint NOT NULL UNIQUE,
  title varchar(200) NOT NULL,
  priority varchar(20) NOT NULL,
  status varchar(20) NOT NULL,
  assignee_id integer,
  due_time varchar(50) NOT NULL,
  accepted_at varchar(50),
  resolved_at varchar(50),
  verified_at varchar(50),
  resolution varchar(1000),
  create_time varchar(50) NOT NULL,
  update_time varchar(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_work_order_status_due ON wvp_ai_work_order(status, due_time);
