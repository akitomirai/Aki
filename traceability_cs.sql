/*
 Navicat Premium Data Transfer

 Source Server         : Akinokaze
 Source Server Type    : MySQL
 Source Server Version : 80039
 Source Host           : localhost:3306
 Source Schema         : traceability_cs

 Target Server Type    : MySQL
 Target Server Version : 80039
 File Encoding         : 65001

 Date: 14/03/2026 00:23:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for base_product
-- ----------------------------
DROP TABLE IF EXISTS `base_product`;
CREATE TABLE `base_product`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `spec` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `unit` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of base_product
-- ----------------------------
INSERT INTO `base_product` VALUES (1, 'Gannan Navel Orange', 'Fruit', '10kg/Box', 'Box', '2026-03-11 22:30:03');
INSERT INTO `base_product` VALUES (2, '????', '??', '2kg', '?', '2026-03-12 13:48:09');
INSERT INTO `base_product` VALUES (3, '????-20260312-135205', '??', '1kg', '?', '2026-03-12 13:52:06');
INSERT INTO `base_product` VALUES (4, '????2-20260312-135232', '??', '2kg', '?', '2026-03-12 13:52:32');
INSERT INTO `base_product` VALUES (5, '????-20260312-145605', '??', '1kg', '?', '2026-03-12 14:56:05');

-- ----------------------------
-- Table structure for consumer_feedback
-- ----------------------------
DROP TABLE IF EXISTS `consumer_feedback`;
CREATE TABLE `consumer_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `qr_id` bigint NULL DEFAULT NULL,
  `feedback_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `source_channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SCAN_PAGE',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `is_public` tinyint(1) NOT NULL DEFAULT 0,
  `handled_by` bigint NULL DEFAULT NULL,
  `handled_at` datetime NULL DEFAULT NULL,
  `handle_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of consumer_feedback
-- ----------------------------
INSERT INTO `consumer_feedback` VALUES (1, 1, 1, 'SUGGESTION', 'Test Feedback - TraceLink Verified', 'Tester', '13800000000', 'SCAN_PAGE', 'PENDING', 0, NULL, NULL, NULL, '2026-03-11 22:46:18', '2026-03-11 22:46:18');
INSERT INTO `consumer_feedback` VALUES (2, 1, 1, 'COMPLAINT', 'Second Feedback - UI Verified', 'AdminTester', '13900000000', 'SCAN_PAGE', 'PENDING', 0, NULL, NULL, NULL, '2026-03-11 22:48:19', '2026-03-11 22:48:19');
INSERT INTO `consumer_feedback` VALUES (3, 1, 1, 'SUGGESTION', 'Security Audit Test', NULL, NULL, 'SCAN_PAGE', 'PENDING', 0, NULL, NULL, NULL, '2026-03-11 23:04:05', '2026-03-11 23:04:05');
INSERT INTO `consumer_feedback` VALUES (4, 1, NULL, 'SUGGESTION', '????', '??', '13800000000', 'SCAN_PAGE', 'CLOSED', 0, 8, '2026-03-12 14:00:37', '???????', '2026-03-12 14:00:36', '2026-03-12 14:00:36');
INSERT INTO `consumer_feedback` VALUES (5, 8, NULL, 'SUGGESTION', '??-????', '??', '13800000000', 'SCAN_PAGE', 'CLOSED', 0, 8, '2026-03-12 14:58:16', '???-??2', '2026-03-12 14:58:15', '2026-03-12 14:58:15');

-- ----------------------------
-- Table structure for hash_notary
-- ----------------------------
DROP TABLE IF EXISTS `hash_notary`;
CREATE TABLE `hash_notary`  (
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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of hash_notary
-- ----------------------------
INSERT INTO `hash_notary` VALUES (1, 'QUALITY_REPORT', 2, '3ca2a0c42a225212081d5142cd680cf36df30d9686c2f02e5d8606a944ff9851', '2026-03-12 13:58:06', 7, '质检报告存证');
INSERT INTO `hash_notary` VALUES (2, 'TRACE_BATCH', 8, 'ba2d7a8a7f5411f67544b848c714711c2c6ed3c17e9ca8147645bf362598b00d', '2026-03-12 14:24:37', 12, '监管召回批次存证');
INSERT INTO `hash_notary` VALUES (3, 'TRACE_BATCH', 9, 'f9cc81187dc748e014ac573d838c03c6854fbf8f764cf181353f975dbf95878b', '2026-03-12 15:07:04', 9, '批次发布存证');
INSERT INTO `hash_notary` VALUES (4, 'QUALITY_REPORT', 3, '196a82c28b3232b679b836f66c1cdc11b1cf94a49d03e305fe9ca016a0ebd1e0', '2026-03-12 14:57:45', 9, '质检报告存证');

-- ----------------------------
-- Table structure for invite_code
-- ----------------------------
DROP TABLE IF EXISTS `invite_code`;
CREATE TABLE `invite_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `invite_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `org_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `org_id` bigint NULL DEFAULT NULL,
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `expire_at` datetime NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'UNUSED',
  `used_by` bigint NULL DEFAULT NULL,
  `used_at` datetime NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of invite_code
-- ----------------------------
INSERT INTO `invite_code` VALUES (1, 'ABC12345', 'ENTERPRISE_USER', 'COMPANY', 1, 'ENTERPRISE_USER', '2026-12-31 23:59:59', 'USED', 5, '2026-03-11 21:23:48', 1, NULL, '2026-03-11 11:08:59', '2026-03-11 21:23:48');
INSERT INTO `invite_code` VALUES (2, 'INVITE_OK_001', 'ENTERPRISE_USER', 'COMPANY', 1, 'ENTERPRISE_USER', '2026-12-31 23:59:59', 'USED', 4, '2026-03-11 21:22:16', 1, '可用邀请码', '2026-03-11 11:13:35', '2026-03-11 21:22:16');
INSERT INTO `invite_code` VALUES (3, 'INVITE_USED_001', 'ENTERPRISE_USER', 'COMPANY', 1, 'ENTERPRISE_USER', '2026-12-31 23:59:59', 'USED', NULL, NULL, 1, '已使用邀请码', '2026-03-11 11:13:35', '2026-03-11 11:13:35');
INSERT INTO `invite_code` VALUES (4, 'INVITE_DISABLED_001', 'ENTERPRISE_USER', 'COMPANY', 1, 'ENTERPRISE_USER', '2026-12-31 23:59:59', 'DISABLED', NULL, NULL, 1, '已禁用邀请码', '2026-03-11 11:13:35', '2026-03-11 11:13:35');
INSERT INTO `invite_code` VALUES (5, 'INVITE_EXPIRED_001', 'ENTERPRISE_USER', 'COMPANY', 1, 'ENTERPRISE_USER', '2024-12-31 23:59:59', 'UNUSED', NULL, NULL, 1, '已过期邀请码', '2026-03-11 11:13:35', '2026-03-11 11:13:35');
INSERT INTO `invite_code` VALUES (6, 'AWR4BNJQ', 'ENTERPRISE_USER', 'COMPANY', 1, 'ENTERPRISE_USER', '2026-12-31 23:59:59', 'DISABLED', 3, '2026-03-11 11:30:03', 1, '测试企业邀请码', '2026-03-11 11:24:25', '2026-03-11 11:35:33');
INSERT INTO `invite_code` VALUES (7, 'K5STQM6M', 'ENTERPRISE_USER', 'COMPANY', 1, 'ENTERPRISE_ADMIN', '2026-03-19 04:27:39', 'USED', 7, '2026-03-12 04:27:39', 1, NULL, '2026-03-12 04:27:39', '2026-03-12 04:27:39');

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint NULL DEFAULT NULL,
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `module` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_id` bigint NULL DEFAULT NULL,
  `request_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `request_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `result_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `old_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `new_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 95 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_log
-- ----------------------------
INSERT INTO `operation_log` VALUES (1, 1, 'platform', 'PLATFORM_ADMIN', 'INVITE_CODE', 'CREATE_INVITE_CODE', 'INVITE_CODE', NULL, '/api/admin/invite-code/create', 'POST', NULL, 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Apifox/1.0.0 (https://apifox.com)', '2026-03-11 11:24:25');
INSERT INTO `operation_log` VALUES (2, 1, 'platform', 'PLATFORM_ADMIN', 'INVITE_CODE', 'DISABLE_INVITE_CODE', 'INVITE_CODE', NULL, '/api/admin/invite-code/disable/6', 'PUT', '[6]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Apifox/1.0.0 (https://apifox.com)', '2026-03-11 11:35:33');
INSERT INTO `operation_log` VALUES (3, NULL, NULL, NULL, 'AUTH', 'USER_REGISTER', 'SYS_USER', NULL, '/api/auth/register', 'POST', '[{\"username\":\"testuser3\",\"password\":\"password123\",\"realName\":\"Test User 3\",\"phone\":\"13800138003\",\"inviteCode\":\"ABC12345\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-11 21:23:48');
INSERT INTO `operation_log` VALUES (4, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":1,\"qrId\":null,\"feedbackType\":\"COMPLAINT\",\"content\":\"Test Annotation Log\",\"contactName\":null,\"contactPhone\":null}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-11 21:23:57');
INSERT INTO `operation_log` VALUES (5, NULL, NULL, NULL, 'AUTH', 'USER_REGISTER', 'SYS_USER', NULL, '/api/auth/register', 'POST', '[{\"username\":\"testuser4\",\"password\":\"******\",\"realName\":\"Test User 4\",\"phone\":\"13800138004\",\"inviteCode\":\"ABC12345\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-11 21:31:05');
INSERT INTO `operation_log` VALUES (6, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":1,\"qrId\":null,\"feedbackType\":\"SUGGESTION\",\"content\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA...(truncated)', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-11 21:31:14');
INSERT INTO `operation_log` VALUES (7, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":1,\"qrId\":1,\"feedbackType\":\"SUGGESTION\",\"content\":\"Test Feedback - TraceLink Verified\",\"contactName\":\"Tester\",\"contactPhone\":\"13800000000\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-11 22:45:35');
INSERT INTO `operation_log` VALUES (8, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":1,\"qrId\":1,\"feedbackType\":\"SUGGESTION\",\"content\":\"Test Feedback - TraceLink Verified\",\"contactName\":\"Tester\",\"contactPhone\":\"13800000000\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-11 22:46:18');
INSERT INTO `operation_log` VALUES (9, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":1,\"qrId\":1,\"feedbackType\":\"COMPLAINT\",\"content\":\"Second Feedback - UI Verified\",\"contactName\":\"AdminTester\",\"contactPhone\":\"13900000000\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-11 22:48:20');
INSERT INTO `operation_log` VALUES (10, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":1,\"qrId\":1,\"feedbackType\":\"SUGGESTION\",\"content\":\"Security Audit Test\",\"contactName\":null,\"contactPhone\":null}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-11 23:04:06');
INSERT INTO `operation_log` VALUES (11, NULL, NULL, NULL, 'AUTH', 'USER_REGISTER', 'SYS_USER', NULL, '/api/auth/register', 'POST', '[{\"username\":\"user5355\",\"password\":\"******\",\"realName\":\"RealTest\",\"phone\":\"13812345649\",\"inviteCode\":\"\"}]', 'FAIL', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:27:12');
INSERT INTO `operation_log` VALUES (12, 1, 'platform', 'PLATFORM_ADMIN', 'INVITE_CODE', 'CREATE_INVITE_CODE', 'INVITE_CODE', NULL, '/api/platform/invite-code/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:27:39');
INSERT INTO `operation_log` VALUES (13, NULL, NULL, NULL, 'AUTH', 'USER_REGISTER', 'SYS_USER', NULL, '/api/auth/register', 'POST', '[{\"username\":\"user5682\",\"password\":\"******\",\"realName\":\"RealTest\",\"phone\":\"13812345659\",\"inviteCode\":\"K5STQM6M\"}]', 'SUCCESS', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:27:40');
INSERT INTO `operation_log` VALUES (14, 1, 'platform', 'PLATFORM_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/platform/user/3/status', 'PUT', '[3,{\"status\":0}]', 'SUCCESS', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:36:00');
INSERT INTO `operation_log` VALUES (15, 1, 'platform', 'PLATFORM_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/platform/user/3/status', 'PUT', '[3,{\"status\":1}]', 'SUCCESS', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:36:01');
INSERT INTO `operation_log` VALUES (16, 1, 'platform', 'PLATFORM_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/platform/user/1/status', 'PUT', '[1,{\"status\":0}]', 'FAIL', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:36:01');
INSERT INTO `operation_log` VALUES (17, 1, 'platform', 'PLATFORM_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/platform/user/3/status', 'PUT', '[3,{\"status\":0}]', 'SUCCESS', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:37:11');
INSERT INTO `operation_log` VALUES (18, 1, 'platform', 'PLATFORM_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/platform/user/3/status', 'PUT', '[3,{\"status\":1}]', 'SUCCESS', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:37:11');
INSERT INTO `operation_log` VALUES (19, 7, 'user5682', 'ENTERPRISE_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/enterprise/user/1/status', 'PUT', '[1,{\"status\":0}]', 'FAIL', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:37:11');
INSERT INTO `operation_log` VALUES (20, 6, 'regulator', 'REGULATOR', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/regulator/user/3/status', 'PUT', '[3,{\"status\":0}]', 'FAIL', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:37:11');
INSERT INTO `operation_log` VALUES (21, 1, 'platform', 'PLATFORM_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/platform/user/1/status', 'PUT', '[1,{\"status\":0}]', 'FAIL', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:37:11');
INSERT INTO `operation_log` VALUES (22, 7, 'user5682', 'ENTERPRISE_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'USER', NULL, '/api/enterprise/user/7/status', 'PUT', '[7,{\"status\":0}]', 'FAIL', NULL, NULL, '127.0.0.1', 'Java-http-client/21.0.4', '2026-03-12 04:37:11');
INSERT INTO `operation_log` VALUES (23, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'USER', 'DELETE_USER', 'SYS_USER', NULL, '/api/enterprise/user/9', 'DELETE', '[9]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:21:16');
INSERT INTO `operation_log` VALUES (24, 10, 'entB_admin', 'ENTERPRISE_ADMIN', 'USER', 'DELETE_USER', 'SYS_USER', NULL, '/api/enterprise/user/10', 'DELETE', '[10]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:21:36');
INSERT INTO `operation_log` VALUES (25, 12, 'reg1', 'REGULATOR', 'REGULATION', 'FREEZE_BATCH', 'TRACE_BATCH', NULL, '/api/regulator/batch/freeze', 'POST', '[{\"batchId\":999999,\"reason\":\"test-freeze\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:21:56');
INSERT INTO `operation_log` VALUES (26, 13, 'reg2', 'REGULATOR', 'REGULATION', 'FREEZE_BATCH', 'TRACE_BATCH', NULL, '/api/regulator/batch/freeze', 'POST', '[{\"batchId\":999998,\"reason\":\"test-freeze\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:22:16');
INSERT INTO `operation_log` VALUES (27, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'DELETE_USER', 'SYS_USER', NULL, '/api/platform/user/8', 'DELETE', '[8]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:25:35');
INSERT INTO `operation_log` VALUES (28, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'RESET_USER_PASSWORD', 'SYS_USER', NULL, '/api/platform/user/8/password/reset', 'PUT', '[8]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:26:07');
INSERT INTO `operation_log` VALUES (29, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'DELETE_USER', 'SYS_USER', NULL, '/api/platform/user/8', 'DELETE', '[8]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:48:05');
INSERT INTO `operation_log` VALUES (30, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'RESET_USER_PASSWORD', 'SYS_USER', NULL, '/api/platform/user/8/password/reset', 'PUT', '[8]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:48:05');
INSERT INTO `operation_log` VALUES (31, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product', 'POST', '[{\"name\":\"????\",\"category\":\"??\",\"spec\":\"1kg\",\"unit\":\"?\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:48:09');
INSERT INTO `operation_log` VALUES (32, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product', 'POST', '[{\"name\":\"????\",\"category\":\"??\",\"spec\":\"1kg\",\"unit\":\"?\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:48:09');
INSERT INTO `operation_log` VALUES (33, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'UPDATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product/2', 'PUT', '[2,{\"name\":\"????\",\"category\":\"??\",\"spec\":\"2kg\",\"unit\":\"?\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:48:09');
INSERT INTO `operation_log` VALUES (34, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:48:09');
INSERT INTO `operation_log` VALUES (35, 10, 'entB_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:48:09');
INSERT INTO `operation_log` VALUES (36, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'DELETE_USER', 'SYS_USER', NULL, '/api/platform/user/8', 'DELETE', '[8]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:51:05');
INSERT INTO `operation_log` VALUES (37, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'RESET_USER_PASSWORD', 'SYS_USER', NULL, '/api/platform/user/8/password/reset', 'PUT', '[8]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:51:05');
INSERT INTO `operation_log` VALUES (38, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'USER', 'DELETE_USER', 'SYS_USER', NULL, '/api/enterprise/user/11', 'DELETE', '[11]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:51:06');
INSERT INTO `operation_log` VALUES (39, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'DELETE_USER', 'SYS_USER', NULL, '/api/platform/user/11', 'DELETE', '[11]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:51:06');
INSERT INTO `operation_log` VALUES (40, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'RESET_USER_PASSWORD', 'SYS_USER', NULL, '/api/platform/user/15/password/reset', 'PUT', '[15]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:51:07');
INSERT INTO `operation_log` VALUES (41, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product', 'POST', '[{\"name\":\"????-20260312-135205\",\"category\":\"??\",\"spec\":\"1kg\",\"unit\":\"?\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:52:06');
INSERT INTO `operation_log` VALUES (42, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product', 'POST', '[{\"name\":\"????-20260312-135205\",\"category\":\"??\",\"spec\":\"1kg\",\"unit\":\"?\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:52:06');
INSERT INTO `operation_log` VALUES (43, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'UPDATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product/36768', 'PUT', '[36768,{\"name\":\"????-20260312-135205\",\"category\":\"??\",\"spec\":\"2kg\",\"unit\":\"?\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:52:06');
INSERT INTO `operation_log` VALUES (44, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product', 'POST', '[{\"name\":\"????2-20260312-135232\",\"category\":\"??\",\"spec\":\"1kg\",\"unit\":\"?\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:52:32');
INSERT INTO `operation_log` VALUES (45, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'UPDATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product/4', 'PUT', '[4,{\"name\":\"????2-20260312-135232\",\"category\":\"??\",\"spec\":\"2kg\",\"unit\":\"?\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:52:32');
INSERT INTO `operation_log` VALUES (46, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:53:31');
INSERT INTO `operation_log` VALUES (47, 10, 'entB_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:53:31');
INSERT INTO `operation_log` VALUES (48, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":0,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":\"ACTIVE\",\"regulationStatus\":null,\"remark\":null,\"publicRemark\":\"????A\",\"internalRemark\":null,\"statusReason\":\"????\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:53:31');
INSERT INTO `operation_log` VALUES (49, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":0,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":null,\"regulationStatus\":null,\"remark\":\"????\",\"publicRemark\":null,\"internalRemark\":null,\"statusReason\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:53:31');
INSERT INTO `operation_log` VALUES (50, 14, 'entA_user', 'ENTERPRISE_USER', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:53:31');
INSERT INTO `operation_log` VALUES (51, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'RESET_USER_PASSWORD', 'SYS_USER', NULL, '/api/platform/user/7/password/reset', 'PUT', '[7]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:54:56');
INSERT INTO `operation_log` VALUES (52, 7, 'user5682', 'ENTERPRISE_ADMIN', 'QUALITY', 'CREATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/create', 'POST', '[{\"batchId\":1,\"reportNo\":\"QA-20260312-135536\",\"agency\":\"?????\",\"result\":\"PASS\",\"reportFileUrl\":\"http://example.com/q.pdf\",\"reportJson\":\"{\\\"ph\\\":6.5}\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:55:37');
INSERT INTO `operation_log` VALUES (53, 7, 'user5682', 'ENTERPRISE_ADMIN', 'QUALITY', 'UPDATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/update', 'PUT', '[{\"id\":0,\"reportNo\":null,\"agency\":null,\"result\":\"FAIL\",\"reportFileUrl\":null,\"reportJson\":\"{\\\"ph\\\":5.8}\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:55:37');
INSERT INTO `operation_log` VALUES (54, 7, 'user5682', 'ENTERPRISE_ADMIN', 'QUALITY', 'CREATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/create', 'POST', '[{\"batchId\":1,\"reportNo\":\"QA-20260312-135805\",\"agency\":\"?????\",\"result\":\"PASS\",\"reportFileUrl\":\"http://example.com/q.pdf\",\"reportJson\":\"{\\\"ph\\\":6.5}\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:58:06');
INSERT INTO `operation_log` VALUES (55, 7, 'user5682', 'ENTERPRISE_ADMIN', 'QUALITY', 'UPDATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/update', 'PUT', '[{\"id\":2,\"reportNo\":null,\"agency\":null,\"result\":\"FAIL\",\"reportFileUrl\":null,\"reportJson\":\"{\\\"ph\\\":5.8}\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 13:58:44');
INSERT INTO `operation_log` VALUES (56, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":1,\"qrId\":0,\"feedbackType\":\"SUGGESTION\",\"content\":\"????\",\"contactName\":\"??\",\"contactPhone\":\"13800000000\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:00:10');
INSERT INTO `operation_log` VALUES (57, 8, 'platform_admin', 'PLATFORM_ADMIN', 'FEEDBACK', 'HANDLE_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/admin/feedback/handle', 'PUT', '[{\"id\":0,\"status\":\"CLOSED\",\"handleResult\":\"???????\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:00:11');
INSERT INTO `operation_log` VALUES (58, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":1,\"qrId\":null,\"feedbackType\":\"SUGGESTION\",\"content\":\"????\",\"contactName\":\"??\",\"contactPhone\":\"13800000000\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:00:36');
INSERT INTO `operation_log` VALUES (59, 8, 'platform_admin', 'PLATFORM_ADMIN', 'FEEDBACK', 'HANDLE_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/admin/feedback/handle', 'PUT', '[{\"id\":4,\"status\":\"CLOSED\",\"handleResult\":\"???????\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:00:37');
INSERT INTO `operation_log` VALUES (60, 12, 'reg1', 'REGULATOR', 'REGULATION', 'FREEZE_BATCH', 'TRACE_BATCH', NULL, '/api/regulator/batch/freeze', 'POST', '[{\"batchId\":1,\"reason\":\"????\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:01:57');
INSERT INTO `operation_log` VALUES (61, 13, 'reg2', 'REGULATOR', 'REGULATION', 'FREEZE_BATCH', 'TRACE_BATCH', NULL, '/api/regulator/batch/freeze', 'POST', '[{\"batchId\":1,\"reason\":\"????\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:03:58');
INSERT INTO `operation_log` VALUES (62, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product', 'POST', '[{\"name\":\"????2-20260312-135232\",\"category\":\"??\",\"spec\":\"2kg\",\"unit\":\"?\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:06:34');
INSERT INTO `operation_log` VALUES (63, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:06:59');
INSERT INTO `operation_log` VALUES (64, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:24:06');
INSERT INTO `operation_log` VALUES (65, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":8,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":null,\"regulationStatus\":null,\"remark\":\"??????\",\"publicRemark\":\"????\",\"internalRemark\":\"????\",\"statusReason\":null}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:24:08');
INSERT INTO `operation_log` VALUES (66, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":8,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":\"ACTIVE\",\"regulationStatus\":null,\"remark\":null,\"publicRemark\":null,\"internalRemark\":null,\"statusReason\":\"????\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:24:10');
INSERT INTO `operation_log` VALUES (67, 12, 'reg1', 'REGULATOR', 'REGULATION', 'FREEZE_BATCH', 'TRACE_BATCH', NULL, '/api/regulator/batch/freeze', 'POST', '[{\"batchId\":8,\"reason\":\"????\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:24:11');
INSERT INTO `operation_log` VALUES (68, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'QUALITY', 'CREATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/create', 'POST', '[{\"batchId\":8,\"reportNo\":\"QA-FZ-142412\",\"agency\":\"?????\",\"result\":\"PASS\",\"reportFileUrl\":null,\"reportJson\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:24:12');
INSERT INTO `operation_log` VALUES (69, 12, 'reg1', 'REGULATOR', 'REGULATION', 'RECALL_BATCH', 'TRACE_BATCH', NULL, '/api/regulator/batch/recall', 'POST', '[{\"batchId\":8,\"reason\":\"??????\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:24:37');
INSERT INTO `operation_log` VALUES (70, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'RESET_USER_PASSWORD', 'SYS_USER', NULL, '/api/platform/user/15/password/reset', 'PUT', '[15]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:04');
INSERT INTO `operation_log` VALUES (71, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'DELETE_USER', 'SYS_USER', NULL, '/api/platform/user/8', 'DELETE', '[8]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:04');
INSERT INTO `operation_log` VALUES (72, 8, 'platform_admin', 'PLATFORM_ADMIN', 'USER', 'RESET_USER_PASSWORD', 'SYS_USER', NULL, '/api/platform/user/8/password/reset', 'PUT', '[8]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:04');
INSERT INTO `operation_log` VALUES (73, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product', 'POST', '[{\"name\":\"????-20260312-145605\",\"category\":\"??\",\"spec\":\"1kg\",\"unit\":\"?\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:05');
INSERT INTO `operation_log` VALUES (74, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'UPDATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product/26496', 'PUT', '[26496,{\"name\":\"????-20260312-145605\",\"category\":\"??\",\"spec\":\"2kg\",\"unit\":\"?\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:05');
INSERT INTO `operation_log` VALUES (75, 8, 'platform_admin', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', NULL, '/api/platform/product', 'POST', '[{\"name\":\"????-20260312-145605\",\"category\":\"??\",\"spec\":\"1kg\",\"unit\":\"?\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:06');
INSERT INTO `operation_log` VALUES (76, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:07');
INSERT INTO `operation_log` VALUES (77, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":0,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":null,\"regulationStatus\":null,\"remark\":\"????-??\",\"publicRemark\":\"??-??\",\"internalRemark\":\"??-??\",\"statusReason\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:07');
INSERT INTO `operation_log` VALUES (78, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":0,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":\"ACTIVE\",\"regulationStatus\":null,\"remark\":null,\"publicRemark\":null,\"internalRemark\":null,\"statusReason\":\"??-??\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:07');
INSERT INTO `operation_log` VALUES (79, 14, 'entA_user', 'ENTERPRISE_USER', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:07');
INSERT INTO `operation_log` VALUES (80, 12, 'reg1', 'REGULATOR', 'REGULATION', 'FREEZE_BATCH', 'TRACE_BATCH', NULL, '/api/regulator/batch/freeze', 'POST', '[{\"batchId\":0,\"reason\":\"????-??\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:09');
INSERT INTO `operation_log` VALUES (81, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":0,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":null,\"regulationStatus\":null,\"remark\":\"????-??\",\"publicRemark\":\"??-??\",\"internalRemark\":\"??-??\",\"statusReason\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:10');
INSERT INTO `operation_log` VALUES (82, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'QUALITY', 'CREATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/create', 'POST', '[{\"batchId\":0,\"reportNo\":\"QA-AC-145611\",\"agency\":\"?????\",\"result\":\"PASS\",\"reportFileUrl\":null,\"reportJson\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:12');
INSERT INTO `operation_log` VALUES (83, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'QUALITY', 'UPDATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/update', 'PUT', '[{\"id\":0,\"reportNo\":null,\"agency\":null,\"result\":\"FAIL\",\"reportFileUrl\":null,\"reportJson\":\"{\\\"k\\\":\\\"v2\\\"}\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:12');
INSERT INTO `operation_log` VALUES (84, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'QUALITY', 'CREATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/create', 'POST', '[{\"batchId\":0,\"reportNo\":\"QA-AC-145611\",\"agency\":\"?????\",\"result\":\"PASS\",\"reportFileUrl\":null,\"reportJson\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:13');
INSERT INTO `operation_log` VALUES (85, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":0,\"qrId\":null,\"feedbackType\":\"SUGGESTION\",\"content\":\"????-??\",\"contactName\":\"??\",\"contactPhone\":\"13800000000\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:16');
INSERT INTO `operation_log` VALUES (86, 8, 'platform_admin', 'PLATFORM_ADMIN', 'FEEDBACK', 'HANDLE_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/admin/feedback/handle', 'PUT', '[{\"id\":0,\"status\":\"CLOSED\",\"handleResult\":\"???-??\"}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:16');
INSERT INTO `operation_log` VALUES (87, 10, 'entB_admin', 'ENTERPRISE_ADMIN', 'FEEDBACK', 'HANDLE_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/enterprise/feedback/handle', 'PUT', '[{\"id\":0,\"status\":\"CLOSED\",\"handleResult\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:56:17');
INSERT INTO `operation_log` VALUES (88, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":8,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":null,\"regulationStatus\":null,\"remark\":\"??-???\",\"publicRemark\":null,\"internalRemark\":null,\"statusReason\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:57:01');
INSERT INTO `operation_log` VALUES (89, 14, 'entA_user', 'ENTERPRISE_USER', 'BATCH', 'CREATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:57:02');
INSERT INTO `operation_log` VALUES (90, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'QUALITY', 'CREATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/create', 'POST', '[{\"batchId\":9,\"reportNo\":\"QA-RT-145744\",\"agency\":\"?????\",\"result\":\"PASS\",\"reportFileUrl\":null,\"reportJson\":null}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:57:45');
INSERT INTO `operation_log` VALUES (91, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'QUALITY', 'UPDATE_QUALITY_REPORT', 'QUALITY_REPORT', NULL, '/api/admin/quality/update', 'PUT', '[{\"id\":3,\"reportNo\":null,\"agency\":null,\"result\":\"FAIL\",\"reportFileUrl\":null,\"reportJson\":\"{\\\"k\\\":\\\"v3\\\"}\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:57:46');
INSERT INTO `operation_log` VALUES (92, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/public/feedback/create', 'POST', '[{\"batchId\":8,\"qrId\":null,\"feedbackType\":\"SUGGESTION\",\"content\":\"??-????\",\"contactName\":\"??\",\"contactPhone\":\"13800000000\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:58:15');
INSERT INTO `operation_log` VALUES (93, 8, 'platform_admin', 'PLATFORM_ADMIN', 'FEEDBACK', 'HANDLE_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/admin/feedback/handle', 'PUT', '[{\"id\":5,\"status\":\"CLOSED\",\"handleResult\":\"???-??2\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:58:16');
INSERT INTO `operation_log` VALUES (94, 10, 'entB_admin', 'ENTERPRISE_ADMIN', 'FEEDBACK', 'HANDLE_FEEDBACK', 'CONSUMER_FEEDBACK', NULL, '/api/enterprise/feedback/handle', 'PUT', '[{\"id\":5,\"status\":\"CLOSED\",\"handleResult\":null}]', 'FAIL', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 14:58:17');
INSERT INTO `operation_log` VALUES (95, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', NULL, '/api/enterprise/batch/update', 'PUT', '[{\"id\":9,\"productId\":null,\"originPlace\":null,\"startDate\":null,\"status\":\"ACTIVE\",\"regulationStatus\":null,\"remark\":null,\"publicRemark\":null,\"internalRemark\":null,\"statusReason\":\"??????\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-12 15:07:04');
INSERT INTO `operation_log` VALUES (96, 1, 'platform', 'PLATFORM_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'SYS_USER', NULL, '/api/platform/user/15/status', 'PUT', '[15,{\"status\":0}]', 'SUCCESS', NULL, NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0', '2026-03-13 18:14:05');
INSERT INTO `operation_log` VALUES (97, 1, 'platform', 'PLATFORM_ADMIN', 'USER', 'UPDATE_USER_STATUS', 'SYS_USER', NULL, '/api/platform/user/15/status', 'PUT', '[15,{\"status\":1}]', 'SUCCESS', NULL, NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0', '2026-03-13 18:14:06');
INSERT INTO `operation_log` VALUES (98, 1, 'platform', 'PLATFORM_ADMIN', 'COMPANY', 'UPDATE_COMPANY_BIZ_ROLES', 'COMPANY', NULL, '/api/platform/company/2/biz-roles', 'PUT', '[2,{\"bizRoles\":[\"PRODUCER\",\"PROCESSOR\"]}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-13 19:24:08');
INSERT INTO `operation_log` VALUES (99, 9, 'entA_admin', 'ENTERPRISE_ADMIN', 'TRACE_EVENT', 'CREATE_TRACE_EVENT', 'TRACE_EVENT', NULL, '/api/enterprise/trace-event/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-13 19:47:30');
INSERT INTO `operation_log` VALUES (100, 3, 'testuser1', 'ENTERPRISE_USER', 'TRACE_EVENT', 'CREATE_TRACE_EVENT', 'TRACE_EVENT', NULL, '/api/enterprise/trace-event/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-13 23:10:00');
INSERT INTO `operation_log` VALUES (101, 3, 'testuser1', 'ENTERPRISE_USER', 'TRACE_EVENT', 'CREATE_TRACE_EVENT', 'TRACE_EVENT', NULL, '/api/enterprise/trace-event/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-13 23:13:43');
INSERT INTO `operation_log` VALUES (102, 3, 'testuser1', 'ENTERPRISE_USER', 'TRACE_EVENT', 'CREATE_TRACE_EVENT', 'TRACE_EVENT', NULL, '/api/enterprise/trace-event/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-13 23:33:15');
INSERT INTO `operation_log` VALUES (103, 3, 'testuser1', 'ENTERPRISE_USER', 'TRACE_EVENT', 'CREATE_TRACE_EVENT', 'TRACE_EVENT', NULL, '/api/enterprise/trace-event/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-14 00:11:02');
INSERT INTO `operation_log` VALUES (104, 3, 'testuser1', 'ENTERPRISE_USER', 'TRACE_EVENT', 'CREATE_TRACE_EVENT', 'TRACE_EVENT', NULL, '/api/enterprise/trace-event/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-14 00:11:23');
INSERT INTO `operation_log` VALUES (105, 3, 'testuser1', 'ENTERPRISE_USER', 'TRACE_EVENT', 'CREATE_TRACE_EVENT', 'TRACE_EVENT', NULL, '/api/enterprise/trace-event/create', 'POST', '[Serialization failed]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-14 00:11:39');
INSERT INTO `operation_log` VALUES (106, 3, 'testuser1', 'ENTERPRISE_USER', 'TRACE_EVENT', 'CREATE_TRACE_EVENT', 'TRACE_EVENT', NULL, '/api/enterprise/trace-event/create', 'POST', '[{\"batchId\":1,\"stage\":\"PRODUCE\",\"bizRole\":\"PRODUCER\",\"title\":\"Log Test Node 4\",\"eventTime\":[2026,3,14,0,13,43],\"location\":\"Test Location\",\"isPublic\":true,\"contentJson\":\"{\\\"fields\\\":{\\\"workType\\\":\\\"Testing Fix Final\\\"}}\",\"attachmentsJson\":\"[]\"}]', 'SUCCESS', NULL, NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', '2026-03-14 00:13:44');

-- ----------------------------
-- Table structure for org_company
-- ----------------------------
DROP TABLE IF EXISTS `org_company`;
CREATE TABLE `org_company`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `license_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `contact` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_license`(`license_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of org_company
-- ----------------------------
INSERT INTO `org_company` VALUES (1, 'Gannan Orange Base', 'LIC123456', NULL, NULL, NULL, '2026-03-11 22:30:03');
INSERT INTO `org_company` VALUES (2, 'Test Company A', 'LIC-A-0001', NULL, NULL, NULL, '2026-03-12 13:08:36');
INSERT INTO `org_company` VALUES (3, 'Test Company B', 'LIC-B-0001', NULL, NULL, NULL, '2026-03-12 13:08:36');

-- ----------------------------
-- Table structure for org_company_biz_role
-- ----------------------------
DROP TABLE IF EXISTS `org_company_biz_role`;
CREATE TABLE `org_company_biz_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `biz_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_org_company_biz_role`(`company_id`, `biz_role`) USING BTREE,
  INDEX `idx_org_company_biz_role_company`(`company_id`) USING BTREE,
  CONSTRAINT `fk_org_company_biz_role_company_id` FOREIGN KEY (`company_id`) REFERENCES `org_company` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of org_company_biz_role
-- ----------------------------
INSERT INTO `org_company_biz_role` VALUES (1, 2, 'PRODUCER', '2026-03-13 19:24:07', '2026-03-13 19:24:07');
INSERT INTO `org_company_biz_role` VALUES (2, 2, 'PROCESSOR', '2026-03-13 19:24:07', '2026-03-13 19:24:07');

-- ----------------------------
-- Table structure for pesticide_record
-- ----------------------------
DROP TABLE IF EXISTS `pesticide_record`;
CREATE TABLE `pesticide_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `pesticide_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `dosage` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `usage_date` date NULL DEFAULT NULL,
  `safe_interval_days` int NULL DEFAULT NULL,
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `record_json` json NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_pesticide_batch`(`batch_id`) USING BTREE,
  CONSTRAINT `fk_pesticide_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pesticide_record
-- ----------------------------

-- ----------------------------
-- Table structure for qr_code
-- ----------------------------
DROP TABLE IF EXISTS `qr_code`;
CREATE TABLE `qr_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `qr_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ACTIVE',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `expired_at` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `generated_by` bigint NULL DEFAULT NULL,
  `last_query_at` datetime NULL DEFAULT NULL,
  `pv` bigint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_qr_token`(`qr_token`) USING BTREE,
  INDEX `fk_qr_batch`(`batch_id`) USING BTREE,
  CONSTRAINT `fk_qr_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qr_code
-- ----------------------------
INSERT INTO `qr_code` VALUES (1, 1, 'test-token-2026', 'ACTIVE', '2026-03-11 22:30:03', NULL, NULL, NULL, NULL, '2026-03-14 00:17:38', 69);

-- ----------------------------
-- Table structure for qr_query_log
-- ----------------------------
DROP TABLE IF EXISTS `qr_query_log`;
CREATE TABLE `qr_query_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `qr_id` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `query_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ua` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_country` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_province` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_lng` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `geo_lat` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `referer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_log_qr`(`qr_id`) USING BTREE,
  INDEX `fk_log_batch`(`batch_id`) USING BTREE,
  CONSTRAINT `fk_log_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_log_qr` FOREIGN KEY (`qr_id`) REFERENCES `qr_code` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qr_query_log
-- ----------------------------
INSERT INTO `qr_query_log` VALUES (3, 1, 1, '2026-03-11 22:30:12', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (4, 1, 1, '2026-03-11 22:32:11', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (5, 1, 1, '2026-03-11 23:19:28', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (6, 1, 1, '2026-03-11 23:48:04', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (7, 1, 1, '2026-03-13 19:48:03', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (8, 1, 1, '2026-03-13 21:00:22', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (9, 1, 1, '2026-03-13 21:00:47', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (10, 1, 1, '2026-03-13 21:13:34', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (11, 1, 1, '2026-03-13 21:24:23', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (12, 1, 1, '2026-03-13 21:36:06', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (13, 1, 1, '2026-03-13 21:40:49', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (14, 1, 1, '2026-03-13 21:41:06', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (15, 1, 1, '2026-03-13 21:41:33', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (16, 1, 1, '2026-03-13 21:41:54', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (17, 1, 1, '2026-03-13 21:42:19', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (18, 1, 1, '2026-03-13 21:42:41', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (19, 1, 1, '2026-03-13 21:43:13', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (20, 1, 1, '2026-03-13 21:43:35', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (21, 1, 1, '2026-03-13 21:45:46', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (22, 1, 1, '2026-03-13 21:47:28', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (23, 1, 1, '2026-03-13 21:48:07', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (24, 1, 1, '2026-03-13 21:48:55', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (25, 1, 1, '2026-03-13 21:50:31', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (26, 1, 1, '2026-03-13 21:50:56', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (27, 1, 1, '2026-03-13 21:51:24', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (28, 1, 1, '2026-03-13 21:51:53', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (29, 1, 1, '2026-03-13 21:52:35', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (30, 1, 1, '2026-03-13 21:53:31', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (31, 1, 1, '2026-03-13 21:54:02', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (32, 1, 1, '2026-03-13 21:54:31', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (33, 1, 1, '2026-03-13 21:54:56', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (34, 1, 1, '2026-03-13 21:55:22', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (35, 1, 1, '2026-03-13 21:55:49', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (36, 1, 1, '2026-03-13 21:56:16', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (37, 1, 1, '2026-03-13 21:56:53', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (38, 1, 1, '2026-03-13 21:57:23', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (39, 1, 1, '2026-03-13 21:57:52', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (40, 1, 1, '2026-03-13 21:58:21', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (41, 1, 1, '2026-03-13 21:59:01', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (42, 1, 1, '2026-03-13 21:59:42', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (43, 1, 1, '2026-03-13 22:00:22', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (44, 1, 1, '2026-03-13 22:00:52', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (45, 1, 1, '2026-03-13 22:01:30', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (46, 1, 1, '2026-03-13 22:02:01', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (47, 1, 1, '2026-03-13 22:04:16', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (48, 1, 1, '2026-03-13 22:04:25', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (49, 1, 1, '2026-03-13 22:05:02', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (50, 1, 1, '2026-03-13 22:06:19', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (51, 1, 1, '2026-03-13 22:06:50', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (52, 1, 1, '2026-03-13 23:02:14', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (53, 1, 1, '2026-03-13 23:02:45', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (54, 1, 1, '2026-03-13 23:03:10', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (55, 1, 1, '2026-03-13 23:03:37', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (56, 1, 1, '2026-03-13 23:04:03', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (57, 1, 1, '2026-03-13 23:04:40', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (58, 1, 1, '2026-03-13 23:10:22', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (59, 1, 1, '2026-03-13 23:11:01', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (60, 1, 1, '2026-03-13 23:14:07', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (61, 1, 1, '2026-03-13 23:15:08', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (62, 1, 1, '2026-03-13 23:31:37', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (63, 1, 1, '2026-03-13 23:42:55', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (64, 1, 1, '2026-03-13 23:44:11', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (65, 1, 1, '2026-03-13 23:45:52', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (66, 1, 1, '2026-03-13 23:46:19', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (67, 1, 1, '2026-03-14 00:01:25', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (68, 1, 1, '2026-03-14 00:02:40', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (69, 1, 1, '2026-03-14 00:02:57', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (70, 1, 1, '2026-03-14 00:09:48', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `qr_query_log` VALUES (71, 1, 1, '2026-03-14 00:17:38', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.7920', NULL, NULL, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for qr_query_stat_day
-- ----------------------------
DROP TABLE IF EXISTS `qr_query_stat_day`;
CREATE TABLE `qr_query_stat_day`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `qr_id` bigint NOT NULL,
  `day` date NOT NULL,
  `pv` bigint NOT NULL DEFAULT 0,
  `uv` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qr_query_stat_day
-- ----------------------------
INSERT INTO `qr_query_stat_day` VALUES (1, 1, '2026-03-11', 4, 4);
INSERT INTO `qr_query_stat_day` VALUES (2, 1, '2026-03-13', 60, 60);
INSERT INTO `qr_query_stat_day` VALUES (3, 1, '2026-03-14', 5, 5);

-- ----------------------------
-- Table structure for quality_report
-- ----------------------------
DROP TABLE IF EXISTS `quality_report`;
CREATE TABLE `quality_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `report_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `agency` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `result` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `report_file_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `report_json` json NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_report_no`(`report_no`) USING BTREE,
  INDEX `fk_report_batch`(`batch_id`) USING BTREE,
  CONSTRAINT `fk_report_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of quality_report
-- ----------------------------
INSERT INTO `quality_report` VALUES (2, 1, 'QA-20260312-135805', '?????', 'FAIL', 'http://example.com/q.pdf', '{\"ph\": 5.8}', '2026-03-12 13:58:05');
INSERT INTO `quality_report` VALUES (3, 9, 'QA-RT-145744', '?????', 'FAIL', NULL, '{\"k\": \"v3\"}', '2026-03-12 14:57:44');

-- ----------------------------
-- Table structure for regulation_record
-- ----------------------------
DROP TABLE IF EXISTS `regulation_record`;
CREATE TABLE `regulation_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `inspector_id` bigint NOT NULL,
  `inspector_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `inspect_time` datetime NOT NULL,
  `inspect_result` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `action_taken` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `attachment_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_regulation_batch`(`batch_id`) USING BTREE,
  CONSTRAINT `fk_regulation_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of regulation_record
-- ----------------------------
INSERT INTO `regulation_record` VALUES (1, 1, 7, 'RegulatorA', '2026-03-13 23:44:01', 'NORMAL', 'Routine Inspection', 'Everything is fine', NULL, '2026-03-13 23:44:01', '2026-03-13 23:44:01');

-- ----------------------------
-- Table structure for regulator_org
-- ----------------------------
DROP TABLE IF EXISTS `regulator_org`;
CREATE TABLE `regulator_org`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `contact` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ENABLED',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of regulator_org
-- ----------------------------
INSERT INTO `regulator_org` VALUES (1, 'Test Regulator 1', 'REG001', NULL, NULL, NULL, 'ENABLED', NULL, '2026-03-11 23:12:58', '2026-03-12 13:08:36');
INSERT INTO `regulator_org` VALUES (3, 'Test Regulator 2', 'REG002', NULL, NULL, NULL, 'ENABLED', NULL, '2026-03-12 13:08:36', '2026-03-12 13:08:36');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
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
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'platform', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', '平台管理员', NULL, 'PLATFORM_ADMIN', NULL, NULL, 1, 0, '2026-03-11 11:08:43', '2026-03-13 17:52:18');
INSERT INTO `sys_user` VALUES (3, 'testuser1', '$2a$10$BMHzgS.7V.aoDs8Jsjm83uxSThHzvGe/Gn8vZY0gcxRQTWDPdBK2a', '测试用户1', '13800138001', 'ENTERPRISE_USER', 1, NULL, 1, 0, '2026-03-11 11:30:03', '2026-03-11 11:30:03');
INSERT INTO `sys_user` VALUES (4, 'testuser2', '$2a$10$SjoVXfNYwU1MaqX13x0uM.9zWMZMMbLMMymN//rpgeC6QeD8oHyaG', 'Test User 2', '13800138002', 'ENTERPRISE_USER', 1, NULL, 1, 0, '2026-03-11 21:22:17', '2026-03-11 21:22:17');
INSERT INTO `sys_user` VALUES (5, 'testuser3', '$2a$10$5YoJfBAt41KCIYX2Astcnej9GsLfnimeJMv.citd4lsbIFpdntvXy', 'Test User 3', '13800138003', 'ENTERPRISE_USER', 1, NULL, 1, 0, '2026-03-11 21:23:48', '2026-03-11 21:23:48');
INSERT INTO `sys_user` VALUES (6, 'regulator', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', 'Test Regulator', NULL, 'REGULATOR', NULL, 1, 1, 0, '2026-03-11 23:13:28', '2026-03-13 17:52:18');
INSERT INTO `sys_user` VALUES (7, 'user5682', '$2a$10$8F7oIdXY2K2YMjr41V8L1O7iL1h1rSDMbxJxOFzdILXiVrICH0oh2', 'RealTest', '13812345659', 'ENTERPRISE_ADMIN', 1, NULL, 1, 0, '2026-03-12 04:27:39', '2026-03-12 04:27:39');
INSERT INTO `sys_user` VALUES (8, 'platform_admin', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', '平台管理员', NULL, 'PLATFORM_ADMIN', NULL, NULL, 1, 0, '2026-03-12 13:09:15', '2026-03-13 17:52:18');
INSERT INTO `sys_user` VALUES (9, 'entA_admin', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', '企业A管理员', NULL, 'ENTERPRISE_ADMIN', 2, NULL, 1, 0, '2026-03-12 13:09:15', '2026-03-13 17:52:18');
INSERT INTO `sys_user` VALUES (10, 'entB_admin', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', '企业B管理员', NULL, 'ENTERPRISE_ADMIN', 3, NULL, 1, 0, '2026-03-12 13:09:15', '2026-03-13 17:52:18');
INSERT INTO `sys_user` VALUES (11, 'entB_user', '$2b$10$YQLz6ctHFAW7MAt20UmJe.A5Br70vxVA3v1hfMZpBO.FnPo.U7Ofq', '企业B操作员', NULL, 'ENTERPRISE_USER', 3, NULL, 1, 1, '2026-03-12 13:09:15', '2026-03-12 13:51:06');
INSERT INTO `sys_user` VALUES (12, 'reg1', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', '监管员1', NULL, 'REGULATOR', NULL, 1, 1, 0, '2026-03-12 13:09:15', '2026-03-13 17:52:18');
INSERT INTO `sys_user` VALUES (13, 'reg2', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', '监管员2', NULL, 'REGULATOR', NULL, 3, 1, 0, '2026-03-12 13:09:15', '2026-03-13 17:52:18');
INSERT INTO `sys_user` VALUES (14, 'entA_user', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', '企业A操作员', NULL, 'ENTERPRISE_USER', 2, NULL, 1, 0, '2026-03-12 13:48:00', '2026-03-13 17:52:18');
INSERT INTO `sys_user` VALUES (15, 'reset_user', '$2a$10$6yQS9SpH.Q63i6DlqpFNfuLAWEpXaHmZSQmHSvQkbFaVJx4dsmAlm', '重置验证用户', NULL, 'ENTERPRISE_USER', 2, NULL, 1, 0, '2026-03-12 13:48:01', '2026-03-12 13:49:54');
INSERT INTO `sys_user` VALUES (16, 'deleted_user', '$2b$10$Rf/SEF4Lu2./2jGWJh.9A.hE8ytbEtDaR3cIztFRRGvMq7heUgX3K', '逻辑删除用户', NULL, 'ENTERPRISE_USER', 2, NULL, 1, 1, '2026-03-12 13:48:02', '2026-03-12 13:49:54');

-- ----------------------------
-- Table structure for trace_batch
-- ----------------------------
DROP TABLE IF EXISTS `trace_batch`;
CREATE TABLE `trace_batch`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  `origin_place` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `start_date` date NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'DRAFT',
  `regulation_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'NONE',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `public_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `internal_remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `published_at` datetime NULL DEFAULT NULL,
  `frozen_at` datetime NULL DEFAULT NULL,
  `recalled_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_batch_code`(`batch_code`) USING BTREE,
  INDEX `fk_batch_product`(`product_id`) USING BTREE,
  INDEX `fk_batch_company`(`company_id`) USING BTREE,
  CONSTRAINT `fk_batch_company` FOREIGN KEY (`company_id`) REFERENCES `org_company` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_batch_product` FOREIGN KEY (`product_id`) REFERENCES `base_product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of trace_batch
-- ----------------------------
INSERT INTO `trace_batch` VALUES (1, 'BATCH20260311001', 1, 1, 'Xinfeng, Ganzhou, Jiangxi', '2026-03-01', 'ACTIVE', 'NORMAL', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-11 22:30:03', '2026-03-11 22:30:03', NULL, NULL);
INSERT INTO `trace_batch` VALUES (8, 'B20260312142405958', 4, 2, '??', '2026-03-12', 'RECALLED', 'RECALLED', '??????', '????', '????', '??????', '2026-03-12 14:24:10', '2026-03-12 14:24:11', '2026-03-12 14:24:37', '2026-03-12 14:24:05', '2026-03-12 14:24:05', 9, 12);
INSERT INTO `trace_batch` VALUES (9, 'B20260312145702149', 4, 2, '??', '2026-03-12', 'ACTIVE', 'NONE', '?????-??-2', NULL, NULL, '??????', '2026-03-12 15:07:04', NULL, NULL, '2026-03-12 14:57:02', '2026-03-12 14:57:02', 14, 9);

-- ----------------------------
-- Table structure for trace_batch_participant
-- ----------------------------
DROP TABLE IF EXISTS `trace_batch_participant`;
CREATE TABLE `trace_batch_participant`  (
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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of trace_batch_participant
-- ----------------------------
INSERT INTO `trace_batch_participant` VALUES (1, 9, 2, 'PRODUCER', 0, 1, '批次创建企业', '2026-03-13 19:25:04', '2026-03-13 19:25:04');
INSERT INTO `trace_batch_participant` VALUES (2, 9, 3, 'TRANSPORTER', 1, 0, '????', '2026-03-13 19:25:04', '2026-03-13 19:25:04');
INSERT INTO `trace_batch_participant` VALUES (3, 9, 3, 'WAREHOUSE', 2, 0, '????', '2026-03-13 19:25:04', '2026-03-13 19:25:04');
INSERT INTO `trace_batch_participant` VALUES (4, 1, 1, 'PRODUCER', 0, 1, '批次创建企业', '2026-03-14 00:11:56', '2026-03-14 00:11:56');

-- ----------------------------
-- Table structure for trace_event
-- ----------------------------
DROP TABLE IF EXISTS `trace_event`;
CREATE TABLE `trace_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `company_id` bigint NULL DEFAULT NULL,
  `biz_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `event_time` datetime NOT NULL,
  `operator_id` bigint NULL DEFAULT NULL,
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'SYSTEM',
  `is_public` tinyint NULL DEFAULT 1,
  `content_json` json NULL,
  `attachments_json` json NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_event_batch`(`batch_id`) USING BTREE,
  CONSTRAINT `fk_event_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of trace_event
-- ----------------------------
INSERT INTO `trace_event` VALUES (1, 1, 1, 'PRODUCER', 'PRODUCE', 'Planting Work', '2026-03-01 08:00:00', NULL, NULL, NULL, 'SYSTEM', 1, '{\"fields\": {\"workType\": \"Sowing\"}}', NULL, '2026-03-11 22:30:03');
INSERT INTO `trace_event` VALUES (10, 8, 2, 'SYSTEM', 'SYSTEM', '创建批次', '2026-03-12 14:24:06', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"批次已创建，当前状态为 DRAFT\"}', '[]', '2026-03-12 14:24:05');
INSERT INTO `trace_event` VALUES (11, 8, 2, 'SYSTEM', 'SYSTEM', '修改批次信息', '2026-03-12 14:24:08', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"已更新批次基础信息\"}', '[]', '2026-03-12 14:24:08');
INSERT INTO `trace_event` VALUES (12, 8, 2, 'SYSTEM', 'SYSTEM', '修改批次信息', '2026-03-12 14:24:10', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"已更新批次基础信息\"}', '[]', '2026-03-12 14:24:09');
INSERT INTO `trace_event` VALUES (13, 8, 2, 'SYSTEM', 'SYSTEM', '批次启用', '2026-03-12 14:24:10', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"批次状态由 DRAFT 变更为 ACTIVE，原因：????\"}', '[]', '2026-03-12 14:24:09');
INSERT INTO `trace_event` VALUES (14, 8, 2, 'SYSTEM', 'SYSTEM', '批次冻结', '2026-03-12 14:24:11', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"批次状态由 ACTIVE 变更为 FROZEN，原因：????\"}', '[]', '2026-03-12 14:24:10');
INSERT INTO `trace_event` VALUES (15, 8, 2, 'SYSTEM', 'SYSTEM', '批次召回', '2026-03-12 14:24:37', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"批次状态由 FROZEN 变更为 RECALLED，原因：??????\"}', '[]', '2026-03-12 14:24:36');
INSERT INTO `trace_event` VALUES (16, 9, 2, 'SYSTEM', 'SYSTEM', '创建批次', '2026-03-12 14:57:02', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"批次已创建，当前状态为 DRAFT\"}', '[]', '2026-03-12 14:57:02');
INSERT INTO `trace_event` VALUES (17, 9, 2, 'SYSTEM', 'SYSTEM', '修改批次信息', '2026-03-12 15:07:04', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"已更新批次基础信息\"}', '[]', '2026-03-12 15:07:04');
INSERT INTO `trace_event` VALUES (18, 9, 2, 'SYSTEM', 'SYSTEM', '批次启用', '2026-03-12 15:07:04', NULL, '系统', NULL, 'SYSTEM', 1, '{\"message\": \"批次状态由 DRAFT 变更为 ACTIVE，原因：??????\"}', '[]', '2026-03-12 15:07:04');
INSERT INTO `trace_event` VALUES (19, 9, 2, 'TRANSPORTER', 'TRANSPORT', '????', '2026-03-13 19:47:30', 9, '企业A管理员', '??', 'ADMIN', 1, '{\"message\": \"?????,?????\"}', '[]', '2026-03-13 19:47:30');
INSERT INTO `trace_event` VALUES (20, 1, 1, 'TRANSPORTER', 'TRANSPORT', 'Transport Record', '2026-03-13 22:30:00', 7, 'RealTest', 'Nanchang', 'ADMIN', 1, '{\"fields\": {\"remark\": \"Cold chain\", \"toAddress\": \"Ganzhou\", \"fromAddress\": \"Nanchang\"}}', '[]', '2026-03-13 23:01:57');
INSERT INTO `trace_event` VALUES (21, 1, 1, 'TRANSPORTER', 'TRANSPORT', '???????', '2026-03-13 23:10:00', 3, '测试用户1', '??????', 'ADMIN', 1, '{\"fields\": {\"remark\": \"?????????\", \"toAddress\": \"?????\", \"fromAddress\": \"??????\"}}', '[]', '2026-03-13 23:10:00');
INSERT INTO `trace_event` VALUES (22, 1, 1, 'TRANSPORTER', 'TRANSPORT', 'Final Test Node', '2026-03-13 23:13:42', 3, '测试用户1', 'Ganzhou Center', 'ADMIN', 1, '{\"fields\": {\"remark\": \"Final check\", \"toAddress\": \"Shenzhen\", \"fromAddress\": \"Ganzhou\"}}', '[]', '2026-03-13 23:13:42');
INSERT INTO `trace_event` VALUES (23, 1, 1, 'PROCESSOR', 'PROCESS', 'Sorting Process', '2026-03-13 23:33:14', 3, '测试用户1', 'Ganzhou Processing Center', 'ADMIN', 1, '{\"fields\": {\"remark\": \"Automated sorting\", \"processName\": \"Sorting\", \"processResult\": \"Grade A\"}}', '[]', '2026-03-13 23:33:14');
INSERT INTO `trace_event` VALUES (24, 1, 1, 'PRODUCER', 'PRODUCE', 'Log Test Node', '2026-03-14 00:11:02', 3, '测试用户1', 'Test Location', 'ADMIN', 1, '{\"fields\": {\"workType\": \"Testing\"}}', '[]', '2026-03-14 00:11:02');
INSERT INTO `trace_event` VALUES (25, 1, 1, 'PRODUCER', 'PRODUCE', 'Log Test Node 2', '2026-03-14 00:11:22', 3, '测试用户1', 'Test Location', 'ADMIN', 1, '{\"fields\": {\"workType\": \"Testing Fix\"}}', '[]', '2026-03-14 00:11:22');
INSERT INTO `trace_event` VALUES (26, 1, 1, 'PRODUCER', 'PRODUCE', 'Log Test Node 3', '2026-03-14 00:11:39', 3, '测试用户1', 'Test Location', 'ADMIN', 1, '{\"fields\": {\"workType\": \"Testing Final Fix\"}}', '[]', '2026-03-14 00:11:39');
INSERT INTO `trace_event` VALUES (27, 1, 1, 'PRODUCER', 'PRODUCE', 'Log Test Node 4', '2026-03-14 00:13:43', 3, '测试用户1', 'Test Location', 'ADMIN', 1, '{\"fields\": {\"workType\": \"Testing Fix Final\"}}', '[]', '2026-03-14 00:13:43');

SET FOREIGN_KEY_CHECKS = 1;
