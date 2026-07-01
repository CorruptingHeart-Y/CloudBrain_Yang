-- ============================================================
-- 东软智慧云脑诊疗平台 - v2.2 病历元数据表
-- 病历来源（A=AI/M=人工）+ AI 快照，不 ALTER medical_record
-- ============================================================
-- 本脚本由 docker-entrypoint-initdb.d 在首次建库时自动执行
-- （排在 01-init-schema.sql → 02-triage-record.sql → 03-prescription-audit-record.sql 之后）。
-- 版本化参考副本见 sql/v2.2_ai_schema_migration.sql。
-- ============================================================

USE `hospital_cloud_brain`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `medical_record_meta` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `medical_record_id` INT(9) NOT NULL COMMENT '病历ID，指向medical_record(id)',
  `source` CHAR(1) DEFAULT 'M' COMMENT '病历来源：A=AI草稿生成，M=人工录入',
  `ai_request_snapshot` TEXT COMMENT 'AI生成时的请求快照JSON',
  `ai_result_snapshot` TEXT COMMENT 'AI生成时的结果快照JSON',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_medical_record_id` (`medical_record_id`),
  INDEX `idx_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病历元数据表';
