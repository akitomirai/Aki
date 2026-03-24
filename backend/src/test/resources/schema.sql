DROP TABLE IF EXISTS batch_status_log;
DROP TABLE IF EXISTS qr_query_log;
DROP TABLE IF EXISTS qr_code;
DROP TABLE IF EXISTS biz_attachment;
DROP TABLE IF EXISTS quality_report;
DROP TABLE IF EXISTS trace_event;
DROP TABLE IF EXISTS trace_batch;
DROP TABLE IF EXISTS org_company;
DROP TABLE IF EXISTS base_product;

CREATE TABLE base_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64),
  spec VARCHAR(64),
  unit VARCHAR(16),
  image_url VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE org_company (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  license_no VARCHAR(64),
  address VARCHAR(255),
  contact VARCHAR(64),
  phone VARCHAR(32),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trace_batch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_code VARCHAR(64) NOT NULL UNIQUE,
  product_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
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
  CONSTRAINT fk_trace_batch_company FOREIGN KEY (company_id) REFERENCES org_company(id)
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
