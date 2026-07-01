-- ============================================================
-- ⚠️ SUPERSEDED — 本迁移依赖 v1.1（已废弃），请勿执行。
--   v2.0_identity_schema_migration.sql 已将 user_account（含 token_version /
--   uk_employee_id / uk_patient_id）整合进建表语句，从 v1.0 直接运行，无需 v1.2。
--   本文件保留仅供历史追溯，禁止在任何环境执行。
-- ============================================================
-- 东软智慧云脑诊疗平台 - v1.2 账号生命周期迁移【候选迁移 · 已废弃】
-- 目标库：hospital_cloud_brain（在 PR1 v1.1 已执行后的库上运行）
--
-- ⚠️ 本脚本是【候选迁移】，当前禁止对远程库执行。仅用于本地隔离验证。
--
-- 背景：PR1 建立的 user_account 仅有 uk_username 唯一约束；
--   employee_id / patient_id 为普通索引（非唯一），且无 token_version 字段。
--   PR5 需要：
--     1. 一个 employee 最多绑定一个 user_account（uk_employee_id）；
--     2. 一个 patient 最多绑定一个 user_account（uk_patient_id）；
--     3. token_version 字段用于“账号所有历史 Token 立即失效”。
--
-- 安全策略：
--   * 不删除/不修改任何既有数据；
--   * 建唯一索引前先检测重复绑定，若存在重复则主动中止迁移，
--     交人工去重后重跑（幂等：token_version 列与索引均可重复执行）；
--   * 检测用存储过程只统计不抛错(CALL 永不失败)，CALL 后立即 DROP，
--     保证成功/失败任何路径都不在数据库遗留 routine；
--   * 失败状态：步骤(1)已加 token_version 列，但步骤(3)(4)唯一索引不会创建；
--     需运行 v1.2_account_lifecycle_verify.sql 查看重复明细，人工去重后重跑本脚本；
--   * 不插入任何 ADMIN/DOCTOR/PATIENT 种子账号；
--   * 不修改 PR1 的 patient 回填规则。
--
-- MySQL 唯一索引允许多个 NULL，兼容未绑定(employee_id/patient_id 为 NULL)的账号。
-- 幂等性：列/索引存在性经 information_schema 判断 + PREPARE/EXECUTE 守卫。
-- ============================================================

USE `hospital_cloud_brain`;

SET NAMES utf8mb4;

-- (1) 增加 token_version 列（若不存在）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'user_account'
    AND column_name = 'token_version'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `user_account` ADD COLUMN `token_version` INT NOT NULL DEFAULT 1 COMMENT ''Token版本号，递增后该账号所有历史Token失效'' AFTER `status`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (2) 重复绑定检测：若同一 employee_id / patient_id 绑定多账号，则中止迁移。
--     存储过程只统计不抛错（CALL 永不失败），CALL 后立即 DROP，保证不遗留 routine；
--     若存在重复，用 PREPARE/EXECUTE 一条非法语句主动中止（不静默删除数据）。
DROP PROCEDURE IF EXISTS `pr5_v12_check_duplicates`;
DELIMITER $$
CREATE PROCEDURE `pr5_v12_check_duplicates`(OUT dup_emp INT, OUT dup_pat INT)
BEGIN
  SELECT COUNT(*) INTO dup_emp
    FROM (SELECT employee_id FROM `user_account`
          WHERE employee_id IS NOT NULL
          GROUP BY employee_id HAVING COUNT(*) > 1) t;
  SELECT COUNT(*) INTO dup_pat
    FROM (SELECT patient_id FROM `user_account`
          WHERE patient_id IS NOT NULL
          GROUP BY patient_id HAVING COUNT(*) > 1) t2;
END$$
DELIMITER ;
CALL `pr5_v12_check_duplicates`(@pr5_dup_emp, @pr5_dup_pat);
DROP PROCEDURE `pr5_v12_check_duplicates`;
-- 过程已清理；@pr5_dup_emp/@pr5_dup_pat 保留检测结果。
-- @pr5_dup>0 时下列 EXECUTE 一条非法 ALTER 主动中止迁移（错误信息含 PR5_V12_DUPLICATE_BINDING_DETECTED_ABORT 标记）。
SET @pr5_dup := @pr5_dup_emp + @pr5_dup_pat;
SET @guard := IF(@pr5_dup = 0,
  'SELECT 1',
  'ALTER TABLE `user_account` PR5_V12_DUPLICATE_BINDING_DETECTED_ABORT');
PREPARE stmt FROM @guard; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (3) 增加 uk_employee_id 唯一索引（若不存在）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'user_account'
    AND index_name = 'uk_employee_id'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `user_account` ADD UNIQUE INDEX `uk_employee_id` (`employee_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (4) 增加 uk_patient_id 唯一索引（若不存在）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'user_account'
    AND index_name = 'uk_patient_id'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `user_account` ADD UNIQUE INDEX `uk_patient_id` (`patient_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 说明：本脚本到此结束。
--   - 未插入任何账号、未改写 PR1 归并规则；
--   - token_version 默认 1，既有账号（若有）保持 1；
--   - 唯一索引保证 employee/patient 最多绑定一个账号，多 NULL 兼容未绑定账号。
-- ============================================================
