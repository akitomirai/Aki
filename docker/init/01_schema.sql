CREATE DATABASE IF NOT EXISTS traceability
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE traceability;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for base_product
-- ----------------------------
CREATE TABLE IF NOT EXISTS `base_product`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `spec` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `unit` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for org_company
-- ----------------------------
CREATE TABLE IF NOT EXISTS `org_company`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `license_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `contact` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_org_company_license_no`(`license_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `org_company_biz_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `biz_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_org_company_biz_role`(`company_id`, `biz_role`) USING BTREE,
  INDEX `idx_org_company_biz_role_company`(`company_id`) USING BTREE,
  CONSTRAINT `fk_org_company_biz_role_company_id` FOREIGN KEY (`company_id`) REFERENCES `org_company` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for regulator_org
-- ----------------------------
CREATE TABLE IF NOT EXISTS `regulator_org`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '监管机构名称',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '监管机构编码',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `contact` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系人',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_regulator_org_code`(`code`) USING BTREE,
  INDEX `idx_regulator_org_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '监管机构表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `company_id` bigint NULL DEFAULT NULL,
  `regulator_org_id` bigint NULL DEFAULT NULL COMMENT '监管机构ID',
  `status` tinyint NOT NULL DEFAULT 1,
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否1是',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_username`(`username`) USING BTREE,
  INDEX `idx_sys_user_company_id`(`company_id`) USING BTREE,
  INDEX `idx_sys_user_regulator_org_id`(`regulator_org_id`) USING BTREE,
  CONSTRAINT `fk_sys_user_company_id` FOREIGN KEY (`company_id`) REFERENCES `org_company` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_sys_user_regulator_org_id` FOREIGN KEY (`regulator_org_id`) REFERENCES `regulator_org` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_batch
-- ----------------------------
CREATE TABLE IF NOT EXISTS `trace_batch`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  `origin_place` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `start_date` date NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `regulation_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `public_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '对外说明',
  `internal_remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内部备注',
  `status_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态变更原因',
  `published_at` datetime NULL DEFAULT NULL COMMENT '启用时间',
  `frozen_at` datetime NULL DEFAULT NULL COMMENT '冻结时间',
  `recalled_at` datetime NULL DEFAULT NULL COMMENT '召回时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_trace_batch_batch_code`(`batch_code`) USING BTREE,
  INDEX `idx_trace_batch_company_id`(`company_id`) USING BTREE,
  INDEX `idx_trace_batch_product_id`(`product_id`) USING BTREE,
  INDEX `idx_trace_batch_status`(`status`) USING BTREE,
  INDEX `idx_trace_batch_regulation_status`(`regulation_status`) USING BTREE,
  INDEX `fk_trace_batch_created_by`(`created_by`) USING BTREE,
  INDEX `fk_trace_batch_updated_by`(`updated_by`) USING BTREE,
  CONSTRAINT `fk_trace_batch_company_id` FOREIGN KEY (`company_id`) REFERENCES `org_company` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_trace_batch_created_by` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_trace_batch_product_id` FOREIGN KEY (`product_id`) REFERENCES `base_product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_trace_batch_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `trace_batch_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  `biz_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage_order` int NULL DEFAULT NULL,
  `is_creator` tinyint(1) NOT NULL DEFAULT 0,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_trace_batch_participant`(`batch_id`, `company_id`, `biz_role`) USING BTREE,
  INDEX `idx_trace_batch_participant_batch`(`batch_id`) USING BTREE,
  INDEX `idx_trace_batch_participant_company`(`company_id`) USING BTREE,
  CONSTRAINT `fk_trace_batch_participant_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_trace_batch_participant_company_id` FOREIGN KEY (`company_id`) REFERENCES `org_company` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qr_code
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qr_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `qr_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expired_at` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态变更原因',
  `generated_by` bigint NULL DEFAULT NULL COMMENT '生成人ID',
  `last_query_at` datetime NULL DEFAULT NULL COMMENT '最后扫码时间',
  `pv` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_qr_code_qr_token`(`qr_token`) USING BTREE,
  INDEX `idx_qr_code_batch_id`(`batch_id`) USING BTREE,
  INDEX `idx_qr_code_status`(`status`) USING BTREE,
  INDEX `fk_qr_code_generated_by`(`generated_by`) USING BTREE,
  CONSTRAINT `fk_qr_code_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_qr_code_generated_by` FOREIGN KEY (`generated_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for consumer_feedback
-- ----------------------------
CREATE TABLE IF NOT EXISTS `consumer_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `qr_id` bigint NULL DEFAULT NULL COMMENT '二维码ID',
  `feedback_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '反馈类型：SUGGESTION/COMPLAINT/REPORT_RISK',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '反馈内容',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `source_channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SCAN_PAGE' COMMENT '来源渠道：SCAN_PAGE/MINI_PROGRAM/APP/WEB/OTHER',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/CLOSED/REJECTED',
  `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否公开展示：1是0否',
  `handled_by` bigint NULL DEFAULT NULL COMMENT '处理人ID',
  `handled_at` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `handle_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理结果',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_consumer_feedback_batch_created_at`(`batch_id`, `created_at`) USING BTREE,
  INDEX `idx_consumer_feedback_status_created_at`(`status`, `created_at`) USING BTREE,
  INDEX `idx_consumer_feedback_qr_id`(`qr_id`) USING BTREE,
  INDEX `idx_consumer_feedback_handled_by`(`handled_by`) USING BTREE,
  CONSTRAINT `fk_consumer_feedback_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_consumer_feedback_handled_by` FOREIGN KEY (`handled_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_consumer_feedback_qr_id` FOREIGN KEY (`qr_id`) REFERENCES `qr_code` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消费者反馈表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for hash_notary
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hash_notary`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `biz_id` bigint NOT NULL,
  `sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_hash_notary_biz_type_biz_id`(`biz_type`, `biz_id`) USING BTREE,
  INDEX `idx_hash_notary_created_by`(`created_by`) USING BTREE,
  CONSTRAINT `fk_hash_notary_created_by` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for invite_code
-- ----------------------------
CREATE TABLE IF NOT EXISTS `invite_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邀请码',
  `invite_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邀请码类型：ENTERPRISE_USER/REGULATOR_USER',
  `org_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '组织类型：COMPANY/REGULATOR_ORG',
  `org_id` bigint NOT NULL COMMENT '组织ID（按org_type指向不同表）',
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '受邀后授予角色',
  `expire_at` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED/USED/EXPIRED/DISABLED',
  `used_by` bigint NULL DEFAULT NULL COMMENT '使用人ID',
  `used_at` datetime NULL DEFAULT NULL COMMENT '使用时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_invite_code_code`(`code`) USING BTREE,
  INDEX `idx_invite_code_status_expire_at`(`status`, `expire_at`) USING BTREE,
  INDEX `idx_invite_code_org`(`org_type`, `org_id`) USING BTREE,
  INDEX `idx_invite_code_used_by`(`used_by`) USING BTREE,
  INDEX `idx_invite_code_created_by`(`created_by`) USING BTREE,
  CONSTRAINT `fk_invite_code_created_by` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_invite_code_used_by` FOREIGN KEY (`used_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '邀请码表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人名称',
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色编码',
  `module` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块',
  `action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作',
  `target_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标类型',
  `target_id` bigint NULL DEFAULT NULL COMMENT '目标ID',
  `request_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求路径',
  `request_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求方法',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求参数',
  `result_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '结果状态',
  `old_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '旧值',
  `new_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '新值',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户代理',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation_log_operator_id`(`operator_id`) USING BTREE,
  INDEX `idx_operation_log_target`(`target_type`, `target_id`) USING BTREE,
  INDEX `idx_operation_log_created_at`(`created_at`) USING BTREE,
  CONSTRAINT `fk_operation_log_operator_id` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '业务操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pesticide_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pesticide_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `pesticide_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `dosage` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `usage_date` date NULL DEFAULT NULL,
  `safe_interval_days` int NULL DEFAULT NULL,
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `record_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pesticide_record_batch_id`(`batch_id`) USING BTREE,
  CONSTRAINT `fk_pesticide_record_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qr_query_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qr_query_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `qr_id` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `query_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ua` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_country` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_province` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_city?` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_lng` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_lat` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `referer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_qr_query_log_qr_time`(`qr_id`, `query_time`) USING BTREE,
  INDEX `idx_qr_query_log_batch_time`(`batch_id`, `query_time`) USING BTREE,
  CONSTRAINT `fk_qr_query_log_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_qr_query_log_qr_id` FOREIGN KEY (`qr_id`) REFERENCES `qr_code` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qr_query_stat_day
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qr_query_stat_day`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `qr_id` bigint NOT NULL,
  `day` date NOT NULL,
  `pv` bigint NOT NULL DEFAULT 0,
  `uv` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_qr_query_stat_day_qr_day`(`qr_id`, `day`) USING BTREE,
  CONSTRAINT `fk_qr_query_stat_day_qr_id` FOREIGN KEY (`qr_id`) REFERENCES `qr_code` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quality_report
-- ----------------------------
CREATE TABLE IF NOT EXISTS `quality_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `report_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `agency` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `result` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `report_file_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `report_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_quality_report_report_no`(`report_no`) USING BTREE,
  INDEX `idx_quality_report_batch_id`(`batch_id`) USING BTREE,
  CONSTRAINT `fk_quality_report_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for regulation_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `regulation_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `inspector_id` bigint NOT NULL COMMENT '检查人ID',
  `inspector_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '检查人姓名',
  `inspect_time` datetime NOT NULL COMMENT '检查时间',
  `inspect_result` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '检查结论',
  `action_taken` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理措施',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `attachment_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附件地址',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_regulation_record_batch_id`(`batch_id`) USING BTREE,
  INDEX `idx_regulation_record_inspect_time`(`inspect_time`) USING BTREE,
  INDEX `fk_regulation_record_inspector_id`(`inspector_id`) USING BTREE,
  CONSTRAINT `fk_regulation_record_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_regulation_record_inspector_id` FOREIGN KEY (`inspector_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '监管检查记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_event
-- ----------------------------
CREATE TABLE IF NOT EXISTS `trace_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `company_id` bigint NULL DEFAULT NULL COMMENT '事件所属企业ID（业务节点参与方）',
  `biz_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务角色：PRODUCER/TRANSPORTER/PROCESSOR/WAREHOUSE/SELLER/INSPECTOR/REGULATOR',
  `stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '事件标题',
  `event_time` datetime NOT NULL,
  `operator_id` bigint NULL DEFAULT NULL,
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人名称',
  `location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM' COMMENT '来源类型：SYSTEM/ADMIN/REGULATOR/SCAN',
  `is_public` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否前台可见：1是0否',
  `content_json` json NOT NULL,
  `attachments_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_trace_event_batch_time`(`batch_id`, `event_time`) USING BTREE,
  INDEX `idx_trace_event_batch_stage`(`batch_id`, `stage`) USING BTREE,
  INDEX `idx_trace_event_company_id`(`company_id`) USING BTREE,
  INDEX `idx_trace_event_operator_id`(`operator_id`) USING BTREE,
  INDEX `idx_trace_event_source_type`(`source_type`, `is_public`) USING BTREE,
  CONSTRAINT `fk_trace_event_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_trace_event_operator_id` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
