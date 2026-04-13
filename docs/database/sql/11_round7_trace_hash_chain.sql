ALTER TABLE `trace_event`
  ADD COLUMN `prev_hash` VARCHAR(64) NULL AFTER `attachments_json`,
  ADD COLUMN `hash` VARCHAR(64) NULL AFTER `prev_hash`;

-- 历史数据可保持为空，应用会在首次校验或追加新记录时按时间顺序补齐缺失的 Hash 链。
