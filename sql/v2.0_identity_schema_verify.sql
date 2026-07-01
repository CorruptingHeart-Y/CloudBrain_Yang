-- ============================================================
-- 东软智慧云脑诊疗平台 - v2.0 身份与账号 schema 迁移结果【只读校验】
-- 目标库：hospital_cloud_brain
-- 仅执行只读 SELECT / information_schema 查询，不修改任何数据。
-- 所有结果为聚合或计数，不输出身份证号、密码哈希、姓名等敏感原始值。
-- ============================================================

USE `hospital_cloud_brain`;

-- ---------- 0. 新表存在性 ----------
SELECT 'patient 表存在' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'patient';

SELECT 'user_account 表存在' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'user_account';

SELECT 'patient_register_link 表存在' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'patient_register_link';

-- ---------- 1. 零 ALTER 既有业务表证明 ----------
SELECT 'register 无 patient_id 列(期望0)' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'register' AND column_name = 'patient_id';

SELECT 'employee 无 role 列(期望0)' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'employee' AND column_name = 'role';

SELECT 'employee 无 token_version 列(期望0)' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'employee' AND column_name = 'token_version';

-- ---------- 2. user_account 约束与字段 ----------
SELECT 'user_account.token_version 列存在' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'user_account' AND column_name = 'token_version';

SELECT 'uk_username 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'user_account' AND index_name = 'uk_username';

SELECT 'uk_employee_id 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'user_account' AND index_name = 'uk_employee_id';

SELECT 'uk_patient_id 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'user_account' AND index_name = 'uk_patient_id';

SELECT 'user_account 行数(初始必须为0)' AS metric, COUNT(*) AS cnt FROM `user_account`;

-- ---------- 3. patient 约束 ----------
SELECT 'patient.uk_card_number 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'patient' AND index_name = 'uk_card_number';

SELECT 'patient 行数' AS metric, COUNT(*) AS cnt FROM `patient`;
SELECT 'patient 重复 card_number(应为0)' AS metric, COUNT(*) AS cnt
FROM (SELECT card_number FROM `patient` WHERE card_number IS NOT NULL GROUP BY card_number HAVING COUNT(*) > 1) t;

-- ---------- 4. patient_register_link 约束 ----------
SELECT 'link.uk_register_id 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'patient_register_link' AND index_name = 'uk_register_id';

SELECT 'link.uk_patient_register 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'patient_register_link' AND index_name = 'uk_patient_register';

SELECT 'link 行数' AS metric, COUNT(*) AS cnt FROM `patient_register_link`;
SELECT '一个 register 多 patient(异常,应为0)' AS metric, COUNT(*) AS cnt
FROM (SELECT register_id FROM `patient_register_link` GROUP BY register_id HAVING COUNT(DISTINCT patient_id) > 1) t;

-- ---------- 5. 历史归并情况 ----------
SELECT 'register 总数' AS metric, COUNT(*) AS cnt FROM `register`;
SELECT 'register 已 link' AS metric, COUNT(*) AS cnt
FROM `register` r WHERE EXISTS (SELECT 1 FROM `patient_register_link` l WHERE l.register_id = r.id);

-- 5.1 未 link：card_number 异常格式(NULL/空/长度非18/格式不符)
SELECT '未link-异常格式' AS metric, COUNT(*) AS cnt
FROM `register` r
WHERE NOT EXISTS (SELECT 1 FROM `patient_register_link` l WHERE l.register_id = r.id)
  AND (r.card_number IS NULL OR TRIM(r.card_number) = ''
       OR CHAR_LENGTH(TRIM(r.card_number)) <> 18
       OR TRIM(r.card_number) NOT REGEXP '^[0-9]{17}[0-9Xx]$');

-- 5.2 未 link：格式合规但冲突(待人工)
SELECT '未link-格式合规但冲突(待人工)' AS metric, COUNT(*) AS cnt
FROM `register` r
JOIN (
  SELECT card_number FROM `register`
  WHERE card_number IS NOT NULL AND TRIM(card_number) <> ''
    AND CHAR_LENGTH(TRIM(card_number)) = 18
    AND TRIM(card_number) REGEXP '^[0-9]{17}[0-9Xx]$'
  GROUP BY card_number
  HAVING COUNT(DISTINCT real_name) > 1 OR COUNT(DISTINCT gender) > 1 OR COUNT(DISTINCT birthdate) > 1
) c ON c.card_number = r.card_number
WHERE NOT EXISTS (SELECT 1 FROM `patient_register_link` l WHERE l.register_id = r.id);

-- 5.3 待人工处理的冲突 card_number 分组数
SELECT '冲突 card_number 分组数(待人工)' AS metric, COUNT(*) AS cnt
FROM (
  SELECT card_number FROM `register`
  WHERE card_number IS NOT NULL AND TRIM(card_number) <> ''
    AND CHAR_LENGTH(TRIM(card_number)) = 18
    AND TRIM(card_number) REGEXP '^[0-9]{17}[0-9Xx]$'
  GROUP BY card_number
  HAVING COUNT(DISTINCT real_name) > 1 OR COUNT(DISTINCT gender) > 1 OR COUNT(DISTINCT birthdate) > 1
) t;
