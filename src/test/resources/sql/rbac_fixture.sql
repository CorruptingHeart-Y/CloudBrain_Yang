-- ============================================================
-- 【仅本地验证 fixture — 禁止进入生产迁移路径，禁止由 docker compose 自动执行】
--
-- 用途：为 PR2 端到端验证在本地隔离库 hospital_rbac_v11 中创建：
--   * 1 个 ADMIN（无关联 employee，验证医生字段为 null 不报错）
--   * 1 个 DOCTOR（关联合法 employee）
--   * 1 个 PATIENT（关联合法 patient，密码使用 MD5 以验证首次登录升级 BCrypt）
--
-- 口令（仅本地测试用）：
--   admin    / admin123
--   doctor01 / doctor123
--   patient01/ patient123  （存储为 MD5，首次登录后由后端升级为 BCrypt）
--
-- 幂等：INSERT ... ON DUPLICATE KEY UPDATE，重复执行会重置状态
--       （含将 patient01 密码重置回 MD5，便于重复验证升级逻辑）。
-- ============================================================
USE `hospital_rbac_v11`;

-- ---------- 字典与关联实体 ----------
INSERT INTO `department` (`id`,`dept_code`,`dept_name`,`dept_type`,`delmark`)
VALUES (1,'SJNK','神经内科','临床科室',1)
ON DUPLICATE KEY UPDATE `dept_name`='神经内科', `delmark`=1;

INSERT INTO `regist_level` (`id`,`regist_code`,`regist_name`,`regist_fee`,`regist_quota`,`sequence_no`,`delmark`)
VALUES (1,'PT','普通号',10.00,100,1,1)
ON DUPLICATE KEY UPDATE `regist_name`='普通号', `delmark`=1;

INSERT INTO `employee` (`id`,`deptment_id`,`regist_level_id`,`scheduling_id`,`realname`,`password`,`delmark`)
VALUES (1,1,1,NULL,'测试医生','$2a$10$placeholder-not-used-employee-password',1)
ON DUPLICATE KEY UPDATE `deptment_id`=1, `regist_level_id`=1, `realname`='测试医生', `delmark`=1;

INSERT INTO `patient` (`id`,`real_name`,`gender`,`card_number`,`birthdate`,`phone`,`home_address`,`delmark`,`create_time`,`update_time`)
VALUES (1,'测试患者','男','210102199001011234','1990-01-01','13800000001','沈阳市测试地址',1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `real_name`='测试患者', `delmark`=1, `update_time`=NOW();

-- ---------- 三个账号 ----------
-- ADMIN：employee_id 为空
INSERT INTO `user_account` (`id`,`username`,`password`,`role`,`employee_id`,`patient_id`,`status`,`delmark`,`create_time`,`update_time`)
VALUES (1,'admin','$2a$10$2B0Kv1y9PgDdZy1jWQ0d9ef0d.1YGfm7TpIRGBSL9ZT6E0apSY2Bi','ADMIN',NULL,NULL,1,1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `password`='$2a$10$2B0Kv1y9PgDdZy1jWQ0d9ef0d.1YGfm7TpIRGBSL9ZT6E0apSY2Bi', `role`='ADMIN', `employee_id`=NULL, `patient_id`=NULL, `status`=1, `delmark`=1, `update_time`=NOW();

-- DOCTOR：employee_id=1
INSERT INTO `user_account` (`id`,`username`,`password`,`role`,`employee_id`,`patient_id`,`status`,`delmark`,`create_time`,`update_time`)
VALUES (2,'doctor01','$2a$10$.dKb4SWWiIMdpHYRD1UQ9.DJC.LkCyEAIj6lati8gbvUJYfA.fqEC','DOCTOR',1,NULL,1,1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `password`='$2a$10$.dKb4SWWiIMdpHYRD1UQ9.DJC.LkCyEAIj6lati8gbvUJYfA.fqEC', `role`='DOCTOR', `employee_id`=1, `patient_id`=NULL, `status`=1, `delmark`=1, `update_time`=NOW();

-- PATIENT：patient_id=1，密码存为 MD5（首次登录后升级为 BCrypt）
INSERT INTO `user_account` (`id`,`username`,`password`,`role`,`employee_id`,`patient_id`,`status`,`delmark`,`create_time`,`update_time`)
VALUES (3,'patient01','c63f24079f1d5e4cae3fdc1a29116a7b','PATIENT',NULL,1,1,1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `password`='c63f24079f1d5e4cae3fdc1a29116a7b', `role`='PATIENT', `employee_id`=NULL, `patient_id`=1, `status`=1, `delmark`=1, `update_time`=NOW();
