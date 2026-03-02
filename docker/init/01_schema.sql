CREATE DATABASE IF NOT EXISTS traceability
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE traceability;

CREATE TABLE IF NOT EXISTS sys_user (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        username VARCHAR(64) NOT NULL UNIQUE,
                                        password VARCHAR(128) NOT NULL,
                                        real_name VARCHAR(64),
                                        phone VARCHAR(32),
                                        role_code VARCHAR(32) NOT NULL,
                                        company_id BIGINT,
                                        status TINYINT DEFAULT 1,
                                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS org_company (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           name VARCHAR(128) NOT NULL,
                                           license_no VARCHAR(64),
                                           address VARCHAR(255),
                                           contact VARCHAR(64),
                                           phone VARCHAR(32),
                                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_product (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            name VARCHAR(128) NOT NULL,
                                            category VARCHAR(64),
                                            spec VARCHAR(64),
                                            unit VARCHAR(16),
                                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trace_batch (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           batch_code VARCHAR(64) NOT NULL UNIQUE,
                                           product_id BIGINT NOT NULL,
                                           company_id BIGINT NOT NULL,
                                           origin_place VARCHAR(128),
                                           start_date DATE,
                                           status VARCHAR(32) DEFAULT 'ACTIVE',
                                           remark VARCHAR(255),
                                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                           updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                           INDEX idx_company (company_id),
                                           INDEX idx_product (product_id)
);

CREATE TABLE IF NOT EXISTS trace_event (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           batch_id BIGINT NOT NULL,
                                           stage VARCHAR(32) NOT NULL,
                                           event_time DATETIME NOT NULL,
                                           operator_id BIGINT,
                                           location VARCHAR(128),
                                           content_json JSON NOT NULL,
                                           attachments_json JSON NULL,
                                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                           INDEX idx_batch_time (batch_id, event_time),
                                           INDEX idx_batch_stage (batch_id, stage)
);

CREATE TABLE IF NOT EXISTS quality_report (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              batch_id BIGINT NOT NULL,
                                              report_no VARCHAR(64),
                                              agency VARCHAR(128),
                                              result VARCHAR(64),
                                              report_file_url VARCHAR(255),
                                              report_json JSON,
                                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                              INDEX idx_batch (batch_id)
);

CREATE TABLE IF NOT EXISTS pesticide_record (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                batch_id BIGINT NOT NULL,
                                                pesticide_name VARCHAR(128),
                                                dosage VARCHAR(64),
                                                usage_date DATE,
                                                safe_interval_days INT,
                                                operator VARCHAR(64),
                                                record_json JSON,
                                                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                INDEX idx_batch2 (batch_id)
);

CREATE TABLE IF NOT EXISTS hash_notary (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           biz_type VARCHAR(32) NOT NULL,
                                           biz_id BIGINT NOT NULL,
                                           sha256 CHAR(64) NOT NULL,
                                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                           created_by BIGINT,
                                           remark VARCHAR(255),
                                           UNIQUE KEY uk_biz (biz_type, biz_id)
);

CREATE TABLE IF NOT EXISTS qr_code (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       batch_id BIGINT NOT NULL,
                                       qr_token VARCHAR(255) NOT NULL UNIQUE,
                                       status VARCHAR(16) DEFAULT 'ACTIVE',
                                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       expired_at DATETIME NULL,
                                       remark VARCHAR(255),
                                       INDEX idx_batch3 (batch_id)
);

CREATE TABLE IF NOT EXISTS qr_query_log (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            qr_id BIGINT NOT NULL,
                                            batch_id BIGINT NOT NULL,
                                            query_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                            ip VARCHAR(64),
                                            ua VARCHAR(255),
                                            geo_country VARCHAR(64),
                                            geo_province VARCHAR(64),
                                            geo_city VARCHAR(64),
                                            geo_lng VARCHAR(32),
                                            geo_lat VARCHAR(32),
                                            referer VARCHAR(255),
                                            INDEX idx_qr_time (qr_id, query_time),
                                            INDEX idx_batch_time2 (batch_id, query_time)
);

CREATE TABLE IF NOT EXISTS qr_query_stat_day (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 qr_id BIGINT NOT NULL,
                                                 day DATE NOT NULL,
                                                 pv BIGINT DEFAULT 0,
                                                 uv BIGINT DEFAULT 0,
                                                 UNIQUE KEY uk_day (qr_id, day)
);