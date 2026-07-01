-- ============================================================
-- 东软智慧云脑诊疗平台 - Docker 首次建库自动执行副本
-- 对应 sql/v1.3_medical_record_source.sql
-- medical_record 增加 source 列（病历来源：A=AI草稿，M=人工）
-- ============================================================
-- 注意：本目录脚本仅首次建库时由 Docker 自动执行一次；现有库需手动跑 sql/v1.3。
--       v1.0 init schema 中 medical_record 建表时尚无 source 列，故建库后补加。
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE `medical_record`
  ADD COLUMN `source` CHAR(1) DEFAULT 'M' COMMENT '病历来源：A=AI草稿生成，M=人工录入'
  AFTER `cure`;
