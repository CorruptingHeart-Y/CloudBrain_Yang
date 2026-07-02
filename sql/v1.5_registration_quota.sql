-- ============================================================
-- v1.5 患者抢号（秒杀架构）- 号源表 + 抢号票据表
-- ============================================================
-- 背景：引入"每个医生每天号数固定"的号源模型，患者自助抢号。
--       RabbitMQ 异步削峰 + Redis Lua 原子扣减 + MySQL 兜底。
--
-- 两张新表：
--   doctor_daily_quota  — ADMIN 按医生+日期+午别放号（capacity/remaining）
--   registration_ticket — 抢号票据，贯穿异步链路状态机(PENDING→SUCCESS/FAILED)
-- ============================================================

USE `hospital_cloud_brain`;
SET NAMES utf8mb4;

-- ---------- 医生每日号源表 ----------
CREATE TABLE IF NOT EXISTS `doctor_daily_quota` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `employee_id` INT(9) NOT NULL COMMENT '医生ID，指向employee(ID)',
  `quota_date` DATE NOT NULL COMMENT '放号日期',
  `noon` VARCHAR(6) NOT NULL COMMENT '午别：上午/下午',
  `capacity` INT(5) NOT NULL COMMENT '总号源',
  `remaining` INT(5) NOT NULL COMMENT '剩余号源',
  `delmark` INT(1) NOT NULL DEFAULT 1 COMMENT '生效标记：1-正常 0-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_date_noon` (`employee_id`,`quota_date`,`noon`),
  KEY `idx_date_noon` (`quota_date`,`noon`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生每日号源表';

-- ---------- 抢号票据表 ----------
-- 幂等：uk_patient_date_noon 保证一患者每半日仅一号(任意状态)。
--       grab() 先查既有 ticket：PENDING/SUCCESS 直接返回；FAILED/CANCELLED 删旧建新。
CREATE TABLE IF NOT EXISTS `registration_ticket` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `ticket_no` VARCHAR(32) NOT NULL COMMENT '票据号 QH+yyyyMMddHHmmss+3位随机',
  `patient_id` INT(9) NOT NULL COMMENT '患者ID，指向patient(ID)',
  `employee_id` INT(9) NOT NULL COMMENT '医生ID',
  `visit_date` DATE NOT NULL COMMENT '就诊日期',
  `noon` VARCHAR(6) NOT NULL COMMENT '午别：上午/下午',
  `regist_level_id` INT(9) NOT NULL COMMENT '挂号级别ID',
  `settle_category_id` INT(9) NOT NULL COMMENT '结算类别ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/CANCELLED',
  `register_id` INT(9) DEFAULT NULL COMMENT '成功后回填挂号ID',
  `fail_reason` VARCHAR(64) DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  UNIQUE KEY `uk_patient_date_noon` (`patient_id`,`visit_date`,`noon`),
  KEY `idx_status` (`status`),
  KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢号票据';

-- 校验
SELECT 'v1.5_tables_added' AS metric, table_name
FROM information_schema.TABLES
WHERE table_schema = DATABASE() AND table_name IN ('doctor_daily_quota','registration_ticket');
