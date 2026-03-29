SET @base_product_add_image_url_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'base_product'
        AND COLUMN_NAME = 'image_url'
    ),
    'SELECT 1',
    'ALTER TABLE `base_product` ADD COLUMN `image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `unit`'
  )
);
PREPARE base_product_add_image_url_stmt FROM @base_product_add_image_url_sql;
EXECUTE base_product_add_image_url_stmt;
DEALLOCATE PREPARE base_product_add_image_url_stmt;

CREATE TABLE IF NOT EXISTS `batch_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operated_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_batch_status_log_batch_id` (`batch_id`),
  KEY `idx_batch_status_log_operated_at` (`operated_at`),
  CONSTRAINT `fk_batch_status_log_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
