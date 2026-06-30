-- ============================================================
-- 东软智慧云脑诊疗平台 - 增量脚本 v1.1
-- 诊前分诊记录表 (triage_record)
-- ============================================================
-- 用途：承接 AI 诊前分诊的推理结果留痕。
-- 患者身份沿用 register.card_number(身份证号)+case_number(病历号)，
-- 挂号前无 register 时直接落主诉/患者简要信息，操作员记 employee_id。
-- 不依赖独立患者表（见 AI 接入决策2）。
-- ============================================================
-- 注意：请在应用所连接的库（master 环境为 hospital_cloud_brain）下执行；
-- v1.0 脚本里的 USE yn 与 application-master.yml 配置不一致，本脚本不再硬切库。
-- 自动执行副本见 docker/mysql/init/02-triage-record.sql（首次建库时由 Docker 自动跑）。
-- ============================================================

SET NAMES utf8mb4;

-- (16) 诊前分诊记录表 (triage_record)
-- 功能说明：记录每次 AI 诊前分诊的输入主诉、推荐科室/医生ID、AI原始返回及操作员，便于复盘与统计。
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
