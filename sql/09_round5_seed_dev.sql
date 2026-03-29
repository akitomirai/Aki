UPDATE `org_company` SET `status` = 'ENABLED' WHERE `status` IS NULL OR `status` = '';

UPDATE `base_product`
SET `product_code` = 'ORANGE-001',
    `origin_place` = 'Jiangxi Ganzhou Xinfeng',
    `status` = 'ENABLED'
WHERE `id` = 1;

UPDATE `base_product`
SET `product_code` = COALESCE(`product_code`, CONCAT('PRODUCT-', `id`)),
    `origin_place` = COALESCE(`origin_place`, 'Jiangxi'),
    `status` = COALESCE(NULLIF(`status`, ''), 'ENABLED');
