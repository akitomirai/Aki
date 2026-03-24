SET @base_product_add_company_id_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'base_product'
        AND COLUMN_NAME = 'company_id'
    ),
    'SELECT 1',
    'ALTER TABLE `base_product` ADD COLUMN `company_id` bigint DEFAULT NULL AFTER `id`'
  )
);
PREPARE base_product_add_company_id_stmt FROM @base_product_add_company_id_sql;
EXECUTE base_product_add_company_id_stmt;
DEALLOCATE PREPARE base_product_add_company_id_stmt;

CREATE TABLE IF NOT EXISTS `biz_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content_type` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size` bigint NOT NULL DEFAULT 0,
  `business_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `business_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_biz_attachment_biz_type_biz_id` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
