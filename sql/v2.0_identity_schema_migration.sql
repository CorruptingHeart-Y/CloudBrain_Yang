-- ============================================================
-- 东软智慧云脑诊疗平台 - v2.0 身份与账号 schema 迁移【候选迁移】
-- 目标库：hospital_cloud_brain（从 v1.0 基线直接运行）
--
-- ⚠️ 本脚本是【候选迁移】，当前禁止对远程库执行。仅用于本地隔离验证。
--
-- 设计原则（与 v1.1/v1.2 的关键差异）：
--   v1.1/v1.2 通过 ALTER register 增加 patient_id 列来承载患者归属，
--   修改了既有业务表结构。v2.0 改为“新增表承载”，**不修改任何既有业务表**：
--     * 不 ALTER / DROP / MODIFY register / employee / prescription / check_request /
--       inspection_request / disposal_request / medical_record 等任何既有表；
--     * 仅 CREATE TABLE / CREATE INDEX（仅新表）/ INSERT...SELECT（仅新表初始化）。
--
-- 新增三张表：
--   1. patient              患者身份主表（card_number 唯一）
--   2. user_account         统一账号表（含 role / token_version / uk_employee_id / uk_patient_id）
--   3. patient_register_link 患者↔挂号 桥接表（一个 register 最多绑定一个 patient）
--
-- 历史归属初始化：
--   仅对“格式合规 + 同 card_number 下 real_name/gender/birthdate 无冲突”的历史挂号
--   创建 patient 并建立 link；冲突 / 异常 / 身份不完整的挂号不建 link，
--   保留给 ADMIN/DOCTOR 业务查询，待人工处理。
--
-- 幂等性：
--   * CREATE TABLE IF NOT EXISTS
--   * INSERT ... ON DUPLICATE KEY UPDATE（patient.uk_card_number / link.uk_register_id）
--     重跑不重复建 patient / link，不覆盖人工已建立的 link，不删除任何既有业务数据。
--
-- 前置条件：目标库为 v1.0 基线（15 张业务表，无 patient/user_account/link）。
--   v1.1 / v1.2 已标记为 superseded，不得与 v2.0 混用。
-- ============================================================

USE `hospital_cloud_brain`;

SET NAMES utf8mb4;

-- ============================================================
-- (1) patient 患者身份主表
-- ============================================================
CREATE TABLE IF NOT EXISTS `patient` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '姓名',
  `gender` VARCHAR(6) DEFAULT NULL COMMENT '性别：男/女',
  `card_number` VARCHAR(18) DEFAULT NULL COMMENT '身份证号（患者唯一身份键）',
  `birthdate` DATE DEFAULT NULL COMMENT '出生日期',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `home_address` VARCHAR(128) DEFAULT NULL COMMENT '家庭住址',
  `delmark` INT DEFAULT 1 COMMENT '生效标记：1-正常 0-已删除',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_card_number` (`card_number`),
  INDEX `idx_real_name` (`real_name`),
  INDEX `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者身份主表';

-- ============================================================
-- (2) user_account 统一账号表（含 token_version 与绑定唯一约束）
--     不给旧 employee 表加 role/token_version/账号字段；角色一律以本表 role 为准。
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_account` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号（唯一）',
  `password` VARCHAR(72) NOT NULL COMMENT '密码（BCrypt 哈希，禁止明文）',
  `role` VARCHAR(16) NOT NULL COMMENT '角色：ADMIN/DOCTOR/PATIENT',
  `employee_id` INT DEFAULT NULL COMMENT '关联员工id，指向employee(id)，DOCTOR/ADMIN 使用',
  `patient_id` INT DEFAULT NULL COMMENT '关联患者id，指向patient(id)，PATIENT 使用',
  `status` INT DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `token_version` INT NOT NULL DEFAULT 1 COMMENT 'Token版本号，递增后该账号所有历史Token失效',
  `delmark` INT DEFAULT 1 COMMENT '生效标记：1-正常 0-已删除',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_username` (`username`),
  UNIQUE INDEX `uk_employee_id` (`employee_id`),
  UNIQUE INDEX `uk_patient_id` (`patient_id`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一认证账号表';

-- ============================================================
-- (3) patient_register_link 患者↔挂号 桥接表
--     一个历史挂号最多绑定一个患者（uk_register_id）；
--     患者查询病历时经此表过滤，不依赖 register.patient_id。
-- ============================================================
CREATE TABLE IF NOT EXISTS `patient_register_link` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `patient_id` INT NOT NULL COMMENT '患者id，指向patient(id)',
  `register_id` INT NOT NULL COMMENT '挂号id，指向register(id)',
  `link_source` VARCHAR(32) DEFAULT 'AUTO_INIT' COMMENT '关联来源：AUTO_INIT(迁移初始化)/AUTO_CREATE(新建挂号自动)/MANUAL(人工)',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_register_id` (`register_id`),
  UNIQUE INDEX `uk_patient_register` (`patient_id`, `register_id`),
  INDEX `idx_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者挂号关联桥接表';

-- ============================================================
-- (4) 历史归并：为“格式合规且无冲突”的 card_number 建立 patient
--     “可自动归并”定义与 v1.1 完全一致：
--       card_number 非空、18 位、^[0-9]{17}[0-9Xx]$，
--       且同 card_number 下 real_name/gender/birthdate 各 DISTINCT 不超过 1。
--     冲突 / 异常 / 身份不完整：不建 patient、不建 link。
-- ============================================================
INSERT INTO `patient` (`real_name`,`gender`,`card_number`,`birthdate`,`home_address`,`delmark`,`create_time`,`update_time`)
SELECT r.real_name, r.gender, r.card_number, r.birthdate, r.home_address, 1, NOW(), NOW()
FROM `register` r
JOIN (
  SELECT MIN(id) AS min_id
  FROM `register`
  WHERE card_number IS NOT NULL
    AND TRIM(card_number) <> ''
    AND CHAR_LENGTH(TRIM(card_number)) = 18
    AND TRIM(card_number) REGEXP '^[0-9]{17}[0-9Xx]$'
  GROUP BY card_number
  HAVING COUNT(DISTINCT real_name) <= 1
     AND COUNT(DISTINCT gender) <= 1
     AND COUNT(DISTINCT birthdate) <= 1
) m ON m.min_id = r.id
ON DUPLICATE KEY UPDATE `update_time` = NOW();

-- ============================================================
-- (5) 历史归并：为可归并 card_number 的全部挂号建立 patient_register_link
--     仅 link 身份与 patient 严格一致的挂号（real_name/gender/birthdate）；
--     ON DUPLICATE KEY(uk_register_id) 保证幂等且不覆盖人工已建立的 link。
-- ============================================================
INSERT INTO `patient_register_link` (`patient_id`,`register_id`,`link_source`,`create_time`)
SELECT p.id, r.id, 'AUTO_INIT', NOW()
FROM `register` r
JOIN `patient` p ON p.card_number = r.card_number
JOIN (
  SELECT card_number
  FROM `register`
  WHERE card_number IS NOT NULL
    AND TRIM(card_number) <> ''
    AND CHAR_LENGTH(TRIM(card_number)) = 18
    AND TRIM(card_number) REGEXP '^[0-9]{17}[0-9Xx]$'
  GROUP BY card_number
  HAVING COUNT(DISTINCT real_name) <= 1
     AND COUNT(DISTINCT gender) <= 1
     AND COUNT(DISTINCT birthdate) <= 1
) m ON m.card_number = r.card_number
WHERE r.real_name <=> p.real_name
  AND r.gender   <=> p.gender
  AND r.birthdate <=> p.birthdate
ON DUPLICATE KEY UPDATE `link_source` = `link_source`;

-- 说明：本脚本到此结束。
--   - 未 ALTER / DROP / MODIFY 任何既有业务表；
--   - 未插入任何 ADMIN/DOCTOR/PATIENT 种子账号；
--   - 冲突 / 异常 card_number 的历史挂号未建 link，待人工处理。
-- ============================================================
