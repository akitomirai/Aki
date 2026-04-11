DROP TABLE IF EXISTS batch_risk_action;
DROP TABLE IF EXISTS batch_status_log;
DROP TABLE IF EXISTS qr_query_log;
DROP TABLE IF EXISTS qr_code;
DROP TABLE IF EXISTS biz_attachment;
DROP TABLE IF EXISTS operation_audit_log;
DROP TABLE IF EXISTS quality_report;
DROP TABLE IF EXISTS trace_event;
DROP TABLE IF EXISTS batch_field_draft;
DROP TABLE IF EXISTS trace_batch;
DROP TABLE IF EXISTS base_product;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS org_company;

CREATE TABLE org_company (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  license_no VARCHAR(64),
  address VARCHAR(255),
  contact VARCHAR(64),
  phone VARCHAR(32),
  status VARCHAR(20) DEFAULT 'ENABLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  real_name VARCHAR(64),
  phone VARCHAR(32),
  role_code VARCHAR(32) NOT NULL,
  company_id BIGINT,
  status TINYINT DEFAULT 1,
  need_change_password TINYINT DEFAULT 0,
  password_updated_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sys_user_company FOREIGN KEY (company_id) REFERENCES org_company(id)
);

CREATE TABLE operation_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_user_id BIGINT,
  operator_name VARCHAR(128),
  role_code VARCHAR(32),
  company_id BIGINT,
  action_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id BIGINT,
  target_name VARCHAR(255),
  result VARCHAR(16) NOT NULL,
  summary VARCHAR(1000),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_operation_audit_log_user FOREIGN KEY (operator_user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_operation_audit_log_company FOREIGN KEY (company_id) REFERENCES org_company(id)
);

CREATE TABLE base_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT,
  product_code VARCHAR(64),
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64),
  origin_place VARCHAR(128),
  spec VARCHAR(64),
  unit VARCHAR(16),
  image_url VARCHAR(255),
  status VARCHAR(20) DEFAULT 'ENABLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_base_product_company FOREIGN KEY (company_id) REFERENCES org_company(id)
);

CREATE TABLE trace_batch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_code VARCHAR(64) NOT NULL UNIQUE,
  product_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  assignee_user_id BIGINT,
  assigned_at TIMESTAMP,
  task_status VARCHAR(20) DEFAULT 'PENDING',
  task_completed_at TIMESTAMP,
  origin_place VARCHAR(128),
  start_date DATE,
  status VARCHAR(20),
  public_remark VARCHAR(500),
  internal_remark VARCHAR(1000),
  status_reason VARCHAR(500),
  published_at TIMESTAMP,
  frozen_at TIMESTAMP,
  recalled_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_trace_batch_product FOREIGN KEY (product_id) REFERENCES base_product(id),
  CONSTRAINT fk_trace_batch_company FOREIGN KEY (company_id) REFERENCES org_company(id),
  CONSTRAINT fk_trace_batch_assignee FOREIGN KEY (assignee_user_id) REFERENCES sys_user(id)
);

CREATE TABLE trace_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  company_id BIGINT,
  stage VARCHAR(32) NOT NULL,
  title VARCHAR(200),
  event_time TIMESTAMP NOT NULL,
  operator_name VARCHAR(100),
  location VARCHAR(128),
  is_public BOOLEAN DEFAULT TRUE,
  content_json CLOB,
  attachments_json CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_trace_event_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id)
);

CREATE TABLE batch_field_draft (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  operator_user_id BIGINT NOT NULL,
  stage VARCHAR(32),
  title VARCHAR(200),
  event_time TIMESTAMP,
  operator_name VARCHAR(100),
  location VARCHAR(128),
  summary VARCHAR(2000),
  image_url VARCHAR(500),
  attachment_ids_json CLOB,
  uploaded_files_json CLOB,
  visible_to_consumer BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_batch_field_draft_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id),
  CONSTRAINT fk_batch_field_draft_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user(id),
  CONSTRAINT uq_batch_field_draft UNIQUE (batch_id, operator_user_id)
);

CREATE TABLE quality_report (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  report_no VARCHAR(64),
  agency VARCHAR(128),
  result VARCHAR(64),
  report_file_url VARCHAR(255),
  report_json CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_quality_report_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id)
);

CREATE TABLE biz_attachment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_url VARCHAR(500),
  content_type VARCHAR(128),
  size BIGINT DEFAULT 0,
  business_type VARCHAR(64) NOT NULL,
  business_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE qr_code (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  qr_token VARCHAR(255) UNIQUE,
  status VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(255),
  status_reason VARCHAR(500),
  expired_at TIMESTAMP,
  last_query_at TIMESTAMP,
  pv BIGINT DEFAULT 0,
  CONSTRAINT fk_qr_code_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id)
);

CREATE TABLE qr_query_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  qr_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  query_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  ip VARCHAR(64),
  ua VARCHAR(1024),
  referer VARCHAR(255),
  CONSTRAINT fk_qr_query_log_qr FOREIGN KEY (qr_id) REFERENCES qr_code(id),
  CONSTRAINT fk_qr_query_log_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id)
);

CREATE TABLE batch_status_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  reason VARCHAR(500),
  operator_name VARCHAR(100),
  operated_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_batch_status_log_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id)
);

CREATE TABLE batch_risk_action (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  reason VARCHAR(500),
  comment VARCHAR(1000),
  operator_name VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_batch_risk_action_batch FOREIGN KEY (batch_id) REFERENCES trace_batch(id)
);
