SET @org_company_add_status_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'org_company'
        AND COLUMN_NAME = 'status'
    ),
    'SELECT 1',
    'ALTER TABLE `org_company` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT ''ENABLED'' AFTER `phone`'
  )
);
PREPARE org_company_add_status_stmt FROM @org_company_add_status_sql;
EXECUTE org_company_add_status_stmt;
DEALLOCATE PREPARE org_company_add_status_stmt;

SET @base_product_add_product_code_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'base_product'
        AND COLUMN_NAME = 'product_code'
    ),
    'SELECT 1',
    'ALTER TABLE `base_product` ADD COLUMN `product_code` varchar(64) DEFAULT NULL AFTER `company_id`'
  )
);
PREPARE base_product_add_product_code_stmt FROM @base_product_add_product_code_sql;
EXECUTE base_product_add_product_code_stmt;
DEALLOCATE PREPARE base_product_add_product_code_stmt;

SET @base_product_add_origin_place_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'base_product'
        AND COLUMN_NAME = 'origin_place'
    ),
    'SELECT 1',
    'ALTER TABLE `base_product` ADD COLUMN `origin_place` varchar(128) DEFAULT NULL AFTER `category`'
  )
);
PREPARE base_product_add_origin_place_stmt FROM @base_product_add_origin_place_sql;
EXECUTE base_product_add_origin_place_stmt;
DEALLOCATE PREPARE base_product_add_origin_place_stmt;

SET @base_product_add_status_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'base_product'
        AND COLUMN_NAME = 'status'
    ),
    'SELECT 1',
    'ALTER TABLE `base_product` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT ''ENABLED'' AFTER `image_url`'
  )
);
PREPARE base_product_add_status_stmt FROM @base_product_add_status_sql;
EXECUTE base_product_add_status_stmt;
DEALLOCATE PREPARE base_product_add_status_stmt;
