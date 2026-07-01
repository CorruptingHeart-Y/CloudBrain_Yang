-- ============================================================
-- 东软智慧云脑诊疗平台 - v2.2 AI 辅助模块 schema 迁移【只读校验】
-- 目标库：hospital_cloud_brain
-- 仅执行只读 SELECT / information_schema 查询，不修改任何数据。
-- 所有结果为聚合或计数，不输出身份证号、姓名等敏感原始值。
-- ============================================================

USE `hospital_cloud_brain`;

-- ---------- 0. 新表存在性 ----------
SELECT 'triage_record 表存在' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'triage_record';

SELECT 'prescription_audit_record 表存在' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'prescription_audit_record';

SELECT 'medical_record_meta 表存在' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'medical_record_meta';

-- ---------- 1. 零 ALTER 既有业务表证明 ----------
SELECT 'medical_record 无 source 列(期望0)' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'medical_record' AND column_name = 'source';

SELECT 'register 无 patient_id 列(期望0)' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'register' AND column_name = 'patient_id';

SELECT 'employee 无 role 列(期望0)' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'employee' AND column_name = 'role';

SELECT 'employee 无 token_version 列(期望0)' AS metric, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'employee' AND column_name = 'token_version';

-- ---------- 2. triage_record 索引 ----------
SELECT 'triage_record.idx_card_number 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'triage_record' AND index_name = 'idx_card_number';

SELECT 'triage_record.idx_operator_employee_id 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'triage_record' AND index_name = 'idx_operator_employee_id';

-- ---------- 3. prescription_audit_record 索引 ----------
SELECT 'audit.idx_register_id 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'prescription_audit_record' AND index_name = 'idx_register_id';

SELECT 'audit.idx_auditor_employee_id 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'prescription_audit_record' AND index_name = 'idx_auditor_employee_id';

-- ---------- 4. medical_record_meta 约束 ----------
SELECT 'meta.uk_medical_record_id 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'medical_record_meta' AND index_name = 'uk_medical_record_id';

SELECT 'meta.idx_source 存在' AS metric, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'medical_record_meta' AND index_name = 'idx_source';

-- ---------- 5. 汇总计数 ----------
SELECT 'v1.0 旧业务表数量(期望15)' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('department','regist_level','settle_category','scheduling','employee',
                     'register','check_request','inspection_request','disposal_request',
                     'medical_technology','medical_record','medical_record_disease',
                     'disease','prescription','drug_info');

SELECT 'v2.0 identity 表数量(期望3)' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('patient','user_account','patient_register_link');

SELECT 'v2.2 AI 表数量(期望3)' AS metric, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('triage_record','prescription_audit_record','medical_record_meta');
