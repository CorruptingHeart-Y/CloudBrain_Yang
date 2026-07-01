-- ============================================================
-- ⚠️ SUPERSEDED — 对应的 v1.1 迁移已被 v2.0 取代，本校验脚本仅供历史追溯，请勿配合 v1.1 执行。
--   新部署使用 v2.0_identity_schema_verify.sql。
-- ============================================================
-- 东软智慧云脑诊疗平台 - v1.1 RBAC 迁移结果【只读校验 · 已废弃】
-- 目标库：hospital_cloud_brain
-- 用法：mysql -h <host> -uroot -p hospital_cloud_brain < sql/v1.1_auth_rbac_verify.sql
-- 说明：仅执行只读 SELECT / information_schema 查询，不修改任何数据。
--       所有结果为聚合或计数，不输出身份证号、密码哈希、姓名等敏感原始值。
-- ============================================================

USE `hospital_cloud_brain`;

-- ---------- 0. 表存在性 ----------
SELECT 'patient 表存在' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'patient';
SELECT 'user_account 表存在' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'user_account';
SELECT 'register.patient_id 列存在' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'register' AND column_name = 'patient_id';

-- ---------- 1. 索引存在性 ----------
SELECT 'register.idx_patient_id 索引存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'register' AND index_name = 'idx_patient_id';
SELECT 'patient.uk_card_number 索引存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'patient' AND index_name = 'uk_card_number';
SELECT 'user_account.uk_username 索引存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'user_account' AND index_name = 'uk_username';

-- ---------- 2. user_account：必须仅有表结构、无任何账号 ----------
SELECT 'user_account 行数(必须为0)' AS metric, COUNT(*) AS cnt FROM `user_account`;
SELECT 'user_account 疑似明文密码(必须为0)' AS metric, COUNT(*) AS cnt
FROM `user_account`
WHERE CHAR_LENGTH(password) <> 60 OR password NOT LIKE '$2%';

-- ---------- 3. patient 数量 ----------
SELECT 'patient 行数' AS metric, COUNT(*) AS cnt FROM `patient`;

-- ---------- 4. register.patient_id 回填情况 ----------
SELECT 'register 总数' AS metric, COUNT(*) AS cnt FROM `register`;
SELECT 'register.patient_id 已回填' AS metric, COUNT(*) AS cnt
FROM `register` WHERE patient_id IS NOT NULL;
SELECT 'register.patient_id 未回填(合计)' AS metric, COUNT(*) AS cnt
FROM `register` WHERE patient_id IS NULL;

-- 4.1 未回填明细：card_number 为空/NULL/长度非18/格式不符（异常格式）
SELECT '未回填-异常格式(NULL/空/长度非18/格式不符)' AS metric, COUNT(*) AS cnt
FROM `register`
WHERE patient_id IS NULL
  AND (card_number IS NULL
       OR TRIM(card_number) = ''
       OR CHAR_LENGTH(TRIM(card_number)) <> 18
       OR TRIM(card_number) NOT REGEXP '^[0-9]{17}[0-9Xx]$');

-- 4.2 未回填明细：格式合规但存在姓名/性别/生日冲突（待人工处理）
SELECT '未回填-格式合规但冲突(待人工)' AS metric, COUNT(*) AS cnt
FROM `register` r
JOIN (
  SELECT card_number
  FROM `register`
  WHERE card_number IS NOT NULL AND TRIM(card_number) <> ''
    AND CHAR_LENGTH(TRIM(card_number)) = 18
    AND TRIM(card_number) REGEXP '^[0-9]{17}[0-9Xx]$'
  GROUP BY card_number
  HAVING COUNT(DISTINCT real_name) > 1
      OR COUNT(DISTINCT gender) > 1
      OR COUNT(DISTINCT birthdate) > 1
) c ON c.card_number = r.card_number
WHERE r.patient_id IS NULL;

-- 4.3 待人工处理的冲突 card_number 分组数
SELECT '冲突 card_number 分组数(待人工)' AS metric, COUNT(*) AS cnt
FROM (
  SELECT card_number
  FROM `register`
  WHERE card_number IS NOT NULL AND TRIM(card_number) <> ''
    AND CHAR_LENGTH(TRIM(card_number)) = 18
    AND TRIM(card_number) REGEXP '^[0-9]{17}[0-9Xx]$'
  GROUP BY card_number
  HAVING COUNT(DISTINCT real_name) > 1
      OR COUNT(DISTINCT gender) > 1
      OR COUNT(DISTINCT birthdate) > 1
) t;

-- ---------- 5. 归并正确性 ----------
-- 5.1 同一 card_number 不应映射到多个 patient_id（异常应为 0）
SELECT '同 card_number 多 patient_id(异常,应为0)' AS metric, COUNT(*) AS cnt
FROM (
  SELECT card_number, COUNT(DISTINCT patient_id) AS c
  FROM `register`
  WHERE card_number IS NOT NULL AND patient_id IS NOT NULL
  GROUP BY card_number
  HAVING c > 1
) t;

-- 5.2 patient 表内重复 card_number（唯一索引保证，应为 0）；仅输出计数，不输出 card_number 本体
SELECT 'patient 重复 card_number(应为0)' AS metric, COUNT(*) AS cnt
FROM (
  SELECT card_number FROM `patient` WHERE card_number IS NOT NULL
  GROUP BY card_number HAVING COUNT(*) > 1
) t;

-- 5.3 每个 patient 的挂号数分布（验证"一患者多次就诊"归并）
SELECT '每个 patient 的挂号数' AS metric, patient_id, COUNT(*) AS cnt
FROM `register` WHERE patient_id IS NOT NULL
GROUP BY patient_id ORDER BY cnt DESC, patient_id LIMIT 20;
