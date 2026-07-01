-- ============================================================
-- 东软智慧云脑诊疗平台 - 增量脚本 v1.3
-- medical_record 增加 source 列（病历来源）
-- ============================================================
-- 用途：区分病历内容由 AI 草稿生成（A）还是人工录入（M）。
-- 触发：Step④ 病历生成——医生粘贴对话文本调 AI 出草稿，编辑确认后落库 source='A'；
--       医生不走 AI 直接手写病历 source='M'。
-- 注意：medical_record 表已存在（v1.0 第 11 张表），本脚本只 ALTER 加列，不建表。
--       请在应用所连接的库（master 环境为 hospital_cloud_brain）下执行；
--       自动执行副本见 docker/mysql/init/04-medical-record-source.sql（首次建库时由 Docker 自动跑）。
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE `medical_record`
  ADD COLUMN `source` CHAR(1) DEFAULT 'M' COMMENT '病历来源：A=AI草稿生成，M=人工录入'
  AFTER `cure`;
