DROP DATABASE IF EXISTS `traceability_cs`;
CREATE DATABASE `traceability_cs` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `traceability_cs`;
SOURCE sql/01_schema.sql;
SOURCE sql/02_seed_dev.sql;
