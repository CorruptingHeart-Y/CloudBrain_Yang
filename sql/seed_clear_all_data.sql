-- ============================================================
-- 清空数据库全部表数据 - hospital_cloud_brain
-- ============================================================
-- 用途：清空当前库下全部 23 张业务/身份/AI 留痕表的数据并重置自增。
--       供 seed_data.sql 重新造数前使用。
--
-- 说明：
--   * schema 不动（不 DROP / 不 ALTER），仅清行。
--   * 本库表间均为逻辑外键（无物理 FOREIGN KEY 约束），TRUNCATE 安全。
--   * 仍包裹 SET FOREIGN_KEY_CHECKS=0 以防个别环境存在物理约束。
--   * TRUNCATE 会自动重置 AUTO_INCREMENT，比 DELETE 更干净。
--   * 执行前请自行备份；不可逆。
-- ============================================================

USE `hospital_cloud_brain`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- AI 留痕表 ----------
TRUNCATE TABLE `triage_record`;
TRUNCATE TABLE `prescription_audit_record`;
TRUNCATE TABLE `medical_record_meta`;

-- ---------- 病历与疾病关联 ----------
TRUNCATE TABLE `medical_record_disease`;
TRUNCATE TABLE `medical_record`;

-- ---------- 医技申请单 ----------
TRUNCATE TABLE `check_request`;
TRUNCATE TABLE `inspection_request`;
TRUNCATE TABLE `disposal_request`;

-- ---------- 处方 ----------
TRUNCATE TABLE `prescription`;

-- ---------- 抢号（号源 + 票据；需在 register/employee 之前清） ----------
TRUNCATE TABLE `registration_ticket`;
TRUNCATE TABLE `doctor_daily_quota`;

-- ---------- 挂号与患者归属桥接 ----------
TRUNCATE TABLE `patient_register_link`;
TRUNCATE TABLE `register`;

-- ---------- 身份与账号 ----------
TRUNCATE TABLE `user_account`;
TRUNCATE TABLE `patient`;
TRUNCATE TABLE `employee`;

-- ---------- 字典与基础 ----------
TRUNCATE TABLE `drug_info`;
TRUNCATE TABLE `disease`;
TRUNCATE TABLE `medical_technology`;
TRUNCATE TABLE `scheduling`;
TRUNCATE TABLE `settle_category`;
TRUNCATE TABLE `regist_level`;
TRUNCATE TABLE `department`;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------- 只读校验：所有表行数应为 0 ----------
SELECT 'table_row_counts_after_clear' AS metric, table_name, 0 AS expected
FROM information_schema.TABLES
WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
ORDER BY table_name;
