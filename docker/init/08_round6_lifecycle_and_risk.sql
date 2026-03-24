CREATE TABLE IF NOT EXISTS `batch_risk_action` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `action_type` varchar(32) NOT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `comment` varchar(1000) DEFAULT NULL,
  `operator_name` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_batch_risk_action_batch_id` (`batch_id`),
  KEY `idx_batch_risk_action_created_at` (`created_at`),
  CONSTRAINT `fk_batch_risk_action_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
