-- ============================================================
-- 【仅本地验证 fixture — 禁止进入生产迁移路径，禁止由 docker compose 自动执行】
--
-- 用途：为 PR5 账号管理/患者注册验证在本地隔离库 hospital_rbac_v11 中创建
--      未绑定账号的员工与患者，供 ADMIN 创建账号、患者注册绑定测试使用。
--
-- 前置：rbac_fixture.sql + pr3_patient_fixture.sql + pr4_doctor_fixture.sql。
--   既有员工 1/2、患者 1/2 均已绑定账号；本脚本新增未绑定的员工 3-6、患者 3-5。
-- 幂等：INSERT ... ON DUPLICATE KEY UPDATE。
-- ============================================================
USE `hospital_rbac_v11`;

-- ---------- 未绑定账号的员工(供 ADMIN 创建 DOCTOR 账号) ----------
INSERT INTO `employee` (`id`,`deptment_id`,`regist_level_id`,`scheduling_id`,`realname`,`password`,`delmark`) VALUES
  (3,1,1,NULL,'测试医生C','$2a$10$placeholder-not-used-employee-password',1),
  (4,1,1,NULL,'测试医生D','$2a$10$placeholder-not-used-employee-password',1),
  (5,1,1,NULL,'测试医生E','$2a$10$placeholder-not-used-employee-password',1),
  (6,1,1,NULL,'测试医生F','$2a$10$placeholder-not-used-employee-password',1),
  (7,1,1,NULL,'测试医生G','$2a$10$placeholder-not-used-employee-password',1)
ON DUPLICATE KEY UPDATE `deptment_id`=1, `regist_level_id`=1, `delmark`=1;

-- ---------- 未绑定账号的患者(供 ADMIN 创建 PATIENT 账号 / 患者注册绑定) ----------
INSERT INTO `patient` (`id`,`real_name`,`gender`,`card_number`,`birthdate`,`phone`,`home_address`,`delmark`,`create_time`,`update_time`) VALUES
  (3,'测试患者C','男','210102200001013333','2000-01-01','13800000003','沈阳市测试地址C',1,NOW(),NOW()),
  (4,'测试患者D','女','210102200002024444','2001-02-02','13800000004','沈阳市测试地址D',1,NOW(),NOW()),
  (5,'测试患者E','男','210102200003035555','2002-03-03','13800000005','沈阳市测试地址E',1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `delmark`=1, `update_time`=NOW();
