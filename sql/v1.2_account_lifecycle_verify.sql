-- ============================================================
-- 东软智慧云脑诊疗平台 - v1.2 账号生命周期迁移结果【只读校验】
-- 目标库：hospital_cloud_brain
-- 用法：mysql -h <host> -uroot -p hospital_cloud_brain < sql/v1.2_account_lifecycle_verify.sql
-- 仅执行只读 SELECT / information_schema 查询，不修改任何数据。
-- ============================================================

USE `hospital_cloud_brain`;

-- ---------- 1. 列与索引存在性 ----------
SELECT 'token_version 列存在' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'user_account' AND column_name = 'token_version';

SELECT 'uk_employee_id 唯一索引存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'user_account' AND index_name = 'uk_employee_id';

SELECT 'uk_patient_id 唯一索引存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'user_account' AND index_name = 'uk_patient_id';

-- ---------- 2. 重复绑定检测（应为 0，否则唯一索引无法建立） ----------
SELECT '重复 employee_id 绑定(应为0)' AS metric, COUNT(*) AS cnt
FROM (
  SELECT employee_id FROM `user_account`
  WHERE employee_id IS NOT NULL
  GROUP BY employee_id HAVING COUNT(*) > 1
) t;

SELECT '重复 patient_id 绑定(应为0)' AS metric, COUNT(*) AS cnt
FROM (
  SELECT patient_id FROM `user_account`
  WHERE patient_id IS NOT NULL
  GROUP BY patient_id HAVING COUNT(*) > 1
) t;

-- ---------- 3. 账号概况（不输出敏感字段） ----------
SELECT '账号总数' AS metric, COUNT(*) AS cnt FROM `user_account`;
SELECT 'token_version 分布(min/max)' AS metric, MIN(token_version) AS mn, MAX(token_version) AS mx FROM `user_account`;
