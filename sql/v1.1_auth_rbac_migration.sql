-- ============================================================
-- ⚠️ SUPERSEDED — 本迁移已被 v2.0_identity_schema_migration.sql 取代，请勿执行。
--   v1.1 通过 ALTER TABLE register ADD COLUMN patient_id 修改了既有业务表结构，
--   与“不修改旧业务表”的最终约束冲突。v2.0 改为新增 patient_register_link 桥接表
--   承载患者↔挂号归属，不动 register。新部署从 v1.0 直接执行 v2.0，不得与 v1.1 混用。
--   本文件保留仅供历史追溯，禁止在任何环境执行。
-- ============================================================
-- 东软智慧云脑诊疗平台 - v1.1 认证与权限(RBAC)数据基础【候选迁移 · 已废弃】
-- 目标库：hospital_cloud_brain（远程 v1.0 同款结构）
--
-- ⚠️ 本脚本是【候选迁移】，当前禁止对远程库执行。仅用于本地隔离验证。
--    待团队负责人明确授权后，方可对远程库应用。
--
-- 依据远程真实结构（MySQL 8.0.45 / hospital_cloud_brain / utf8mb4_0900_ai_ci）：
--   远程当前无 patient、user_account、role、register.patient_id。
--
-- 本脚本只做增量新增，不删除/不重建/不改名任何既有业务表：
--   1. 新建 patient（患者主索引）表 —— 仅建表，不生成任何患者登录账号；
--   2. 新建 user_account（统一认证账号）表 —— 仅建表，不插入任何账号/密码/角色；
--   3. 给 register 增加可空 patient_id 列 + 索引（幂等）；
--   4. 仅对【格式合规】且【姓名/性别/生日无冲突】的 18 位 card_number
--      自动创建 patient 并回填 register.patient_id；
--   5. 12 位等异常 card_number、以及存在冲突的 card_number，patient_id 保持 NULL；
--   6. 不依据 employee 自动生成 user_account；不依据历史 register 生成患者账号；
--      不写任何生产账号、密码、角色种子数据。
--
-- 幂等性说明（逐条，不虚称）：
--   * CREATE TABLE IF NOT EXISTS patient / user_account
--       - 首次执行：建表；重复执行：检测已存在，跳过（幂等）。
--   * ALTER register ADD COLUMN patient_id / ADD INDEX idx_patient_id
--       - MySQL 8.0 不支持 ADD COLUMN/INDEX IF NOT EXISTS（该语法为 MariaDB 专有），
--         故用 information_schema 存在性判断 + PREPARE/EXECUTE 守卫；
--       - 首次执行：加列/加索引；重复执行：检测已存在，执行 'SELECT 1' 占位（幂等）。
--       - 兼容 MySQL 8.0.x（远程为 8.0.45）。
--   * INSERT ... SELECT ... ON DUPLICATE KEY UPDATE（建 patient）
--       - 首次执行：为每个可归并 card_number 建一条 patient；
--       - 重复执行：card_number 唯一键命中 ON DUPLICATE，仅刷新 update_time，不产生重复 patient（幂等）。
--   * UPDATE register SET patient_id=... WHERE patient_id IS NULL
--       - 首次执行：回填可归并记录；重复执行：已回填记录被 WHERE patient_id IS NULL 排除，
--         异常/冲突记录无对应 patient(JOIN 不命中) 不更新（幂等）。
--
-- 执行方式（mysql 客户端，脚本未使用 DELIMITER/存储过程）：
--   mysql -h <host> -uroot -p hospital_cloud_brain < sql/v1.1_auth_rbac_migration.sql
-- ============================================================

USE `hospital_cloud_brain`;

SET NAMES utf8mb4;

-- ============================================================
-- (1) 新建 patient 患者主索引表（仅建表，不生成账号）
--     说明：register 仅嵌入每次就诊的患者快照，无法稳定表达"同一患者多次就诊"。
--           patient 以 card_number 为唯一身份键，register.patient_id 指向本表。
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者主索引表';

-- ============================================================
-- (2) 新建 user_account 统一认证账号表（仅建表，不插入任何账号）
--     说明：承载 ADMIN/DOCTOR/PATIENT 三种账号，与 employee、patient 解耦。
--           role 为平级枚举（不做角色继承）。
--           本脚本不为其插入任何数据；账号创建、角色分配、密码设置
--           均由后续 PR 与团队人工决策完成，绝不自动猜测。
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_account` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号（唯一）',
  `password` VARCHAR(72) NOT NULL COMMENT '密码（BCrypt 哈希，禁止明文）',
  `role` VARCHAR(16) NOT NULL COMMENT '角色：ADMIN/DOCTOR/PATIENT',
  `employee_id` INT DEFAULT NULL COMMENT '关联员工id，指向employee(id)，DOCTOR/ADMIN 使用',
  `patient_id` INT DEFAULT NULL COMMENT '关联患者id，指向patient(id)，PATIENT 使用',
  `status` INT DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `delmark` INT DEFAULT 1 COMMENT '生效标记：1-正常 0-已删除',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_username` (`username`),
  INDEX `idx_employee_id` (`employee_id`),
  INDEX `idx_patient_id` (`patient_id`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一认证账号表';

-- ============================================================
-- (3) 给 register 增加可空 patient_id 列 + 索引（幂等）
--     保留 real_name / card_number 等原有冗余字段，不破坏旧接口；
--     本脚本仅建列与回填，查询逻辑改用 patient_id 留待后续 PR。
-- ============================================================

-- 3.1 增加 patient_id 列（若不存在）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'register'
    AND column_name = 'patient_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `register` ADD COLUMN `patient_id` INT DEFAULT NULL COMMENT ''患者id，指向patient(id)'' AFTER `card_number`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.2 增加 idx_patient_id 索引（若不存在）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'register'
    AND index_name = 'idx_patient_id'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `register` ADD INDEX `idx_patient_id` (`patient_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- (4) 历史回填：按 register.card_number 聚合创建 patient 并回填 register.patient_id
--
--     "格式合规"定义（保守，不等于"身份真实有效"）：
--       card_number 非空、去空格后长度 = 18、且匹配 ^[0-9]{17}[0-9Xx]$。
--
--     "可自动归并"定义：
--       格式合规，且同一 card_number 下所有 register 的
--       real_name / gender / birthdate 各自取 DISTINCT 后不超过 1 个非空值
--       （COUNT(DISTINCT) 忽略 NULL，故 NULL 不视为冲突）。
--
--     处理规则：
--       * 可自动归并：取该 card_number 下 MIN(id) 的 register 信息建立一条 patient，
--         并将该 card_number 的全部 register 回填为同一 patient_id；
--       * 12 位等异常格式：不建 patient、不回填，patient_id 保持 NULL；
--       * 格式合规但存在姓名/性别/生日冲突：不建 patient、不回填，patient_id 保持 NULL，
--         留待人工处理（校验脚本会单独统计此类记录）。
-- ============================================================

-- 4.1 为"格式合规且无冲突"的 card_number 建立 patient
--     （card_number 唯一键 + ON DUPLICATE KEY 保证重复执行不产生重复 patient）
INSERT INTO `patient` (`real_name`, `gender`, `card_number`, `birthdate`, `home_address`, `delmark`, `create_time`, `update_time`)
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

-- 4.2 回填 register.patient_id
--     与 4.1 使用【完全相同】的"可自动归并 card_number"条件（格式合规 + 无冲突），
--     通过派生表 m 显式限定。这样即使有人手工为冲突/异常 card_number 插入了 patient，
--     冲突组也绝不会被回填（patient_id 保持 NULL）。
--     派生表 m 的 WHERE/HAVING 与 4.1 子查询逐字一致；MySQL 8.0 会先物化派生表，
--     不触发"不能在 UPDATE 中引用目标表"的 1093 限制。
UPDATE `register` r
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
JOIN `patient` p ON p.card_number = r.card_number
SET r.patient_id = p.id
WHERE r.patient_id IS NULL;

-- 说明：本脚本到此结束。
--   - 未创建任何 user_account 行、未生成任何账号/密码/角色；
--   - 未依据 employee 推断 ADMIN/DOCTOR；
--   - 未依据历史 register 生成患者登录账号；
--   - 上述均留待团队人工决策与后续 PR。
-- ============================================================
