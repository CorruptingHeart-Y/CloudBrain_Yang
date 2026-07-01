-- ============================================================
-- 东软智慧云脑诊疗平台 - v2.2 AI 辅助模块 schema 迁移
-- 目标库：hospital_cloud_brain（v1.0 基线后单独执行）
--
-- 设计约束：
--   本迁移仅 CREATE TABLE IF NOT EXISTS，不 ALTER/DROP/MODIFY
--   任何既有业务表（包括 medical_record / register / employee / prescription）。
--   病历来源（A=AI草稿/M=人工）走 medical_record_meta 新表承载，
--   不修改 medical_record 表结构。
--
-- 新增三张表：
--   1. triage_record             诊前分诊记录（承接 AI 分诊推理结果留痕）
--   2. prescription_audit_record 处方审核记录（承接 AI 审核快照与结果留痕）
--   3. medical_record_meta       病历元数据（source + AI 请求/结果快照，
--                                   medical_record_id 唯一）
--
-- 幂等性：全部 CREATE TABLE IF NOT EXISTS，重跑不报错、不删数据。
-- 前置条件：v1.0 15 张旧业务表已建（docker-compose 首次启动自动完成）。
-- 执行顺序：本脚本可与 v2.0 identity migration 以任意先后顺序运行，
--          二者互不依赖、互不影响。
-- ============================================================

USE `hospital_cloud_brain`;

SET NAMES utf8mb4;

-- ============================================================
-- (1) 诊前分诊记录表 (triage_record)
--     承接 AI 诊前分诊推理结果留痕；患者身份沿用 card_number+case_number，
--     挂号前无 register 时直接落主诉/患者简要信息，操作员记 employee_id。
-- ============================================================
CREATE TABLE IF NOT EXISTS `triage_record` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `card_number` VARCHAR(18) DEFAULT NULL COMMENT '患者身份证号',
  `case_number` VARCHAR(64) DEFAULT NULL COMMENT '病历号',
  `patient_name` VARCHAR(64) DEFAULT NULL COMMENT '患者姓名',
  `gender` VARCHAR(6) DEFAULT NULL COMMENT '性别：男/女',
  `age` INT(3) DEFAULT NULL COMMENT '年龄',
  `chief_complaint` VARCHAR(512) DEFAULT NULL COMMENT '主诉',
  `recommend_dept_ids` VARCHAR(255) DEFAULT NULL COMMENT '推荐科室ID列表(逗号分隔)',
  `recommend_doctor_ids` VARCHAR(255) DEFAULT NULL COMMENT '推荐医生ID列表(逗号分隔)',
  `ai_raw_result` TEXT COMMENT 'AI完整返回JSON(含reason/score)',
  `operator_employee_id` INT(9) DEFAULT NULL COMMENT '操作员员工ID，指向employee(ID)',
  `creation_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_card_number` (`card_number`),
  INDEX `idx_operator_employee_id` (`operator_employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊前分诊记录表';

-- ============================================================
-- (2) 处方审核记录表 (prescription_audit_record)
--     承接 AI 处方辅助审核输入快照与结果留痕；register_id 指向挂号，
--     auditor_employee_id 记操作员。
-- ============================================================
CREATE TABLE IF NOT EXISTS `prescription_audit_record` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `register_id` INT(9) DEFAULT NULL COMMENT '挂号ID，指向register(ID)',
  `request_snapshot` TEXT COMMENT '审核请求快照JSON(患者信息+药品明细)',
  `result_json` TEXT COMMENT 'AI完整返回JSON(含suggestions/interactions/riskItems)',
  `risk_level` VARCHAR(16) DEFAULT NULL COMMENT '总体风险等级：low/medium/high',
  `auditor_employee_id` INT(9) DEFAULT NULL COMMENT '审核操作员员工ID，指向employee(ID)',
  `creation_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_register_id` (`register_id`),
  INDEX `idx_auditor_employee_id` (`auditor_employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方审核记录表';

-- ============================================================
-- (3) 病历元数据表 (medical_record_meta)
--     每个病历（medical_record.id）最多一行。
--     source 区分 AI 草稿生成（A）或人工录入（M），替代原 v1.3 计划的
--     medical_record.source 列。AI 快照字段可选，用于复盘追溯。
-- ============================================================
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

-- 说明：本脚本到此结束。
--   - 仅 CREATE TABLE IF NOT EXISTS 三张新表，不 ALTER 任何既有业务表；
--   - medical_record.source 列不存在——来源信息完全由 medical_record_meta 承载；
--   - 与 v2.0 identity migration（patient/user_account/patient_register_link）互不依赖。
-- ============================================================
