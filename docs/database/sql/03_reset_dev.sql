DROP DATABASE IF EXISTS `traceability_cs`;
CREATE DATABASE `traceability_cs` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `traceability_cs`;
SOURCE 01_schema.sql;
SOURCE 02_seed_dev.sql;
