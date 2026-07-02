-- ============================================================
-- 【仅本地验证 fixture — 禁止进入生产迁移路径，禁止由 docker compose 自动执行】
--
-- 用途：为 PR4 管理员/医生权限矩阵 + 医生数据范围隔离验证，在本地隔离库
--      hospital_rbac_v11 中创建第二名医生及其接诊挂号，供跨医生归属测试。
--
-- 前置：rbac_fixture.sql（employee id=1 / doctor01 / patient1）+
--       pr3_patient_fixture.sql（register 1,2,3 归 doctor A(employee_id=1)；register 4 patient_id NULL）。
-- 本脚本新增：
--   * employee id=2（测试医生B）
--   * user_account id=5 doctor02（DOCTOR，employee_id=2，BCrypt 密码=doctor123，与 doctor01 同hash复用）
--   * register id=10（employee_id=2 即医生B接诊，patient_id=2）
--   * prescription id=20 / check_request id=20（均 register_id=10，属医生B）
--
-- 口令（仅本地测试用）：doctor02 / doctor123
-- 幂等：INSERT ... ON DUPLICATE KEY UPDATE。
-- ============================================================
USE `hospital_rbac_v11`;

-- ---------- 第二名医生(employee id=2) ----------
INSERT INTO `employee` (`id`,`deptment_id`,`regist_level_id`,`scheduling_id`,`realname`,`password`,`delmark`)
VALUES (2,1,1,NULL,'测试医生B','$2a$10$placeholder-not-used-employee-password',1)
ON DUPLICATE KEY UPDATE `deptment_id`=1, `regist_level_id`=1, `realname`='测试医生B', `delmark`=1;

-- ---------- doctor02 账号(DOCTOR, employee_id=2, BCrypt=doctor123，复用 doctor01 的hash) ----------
INSERT INTO `user_account` (`id`,`username`,`password`,`role`,`employee_id`,`patient_id`,`status`,`delmark`,`create_time`,`update_time`)
VALUES (5,'doctor02','$2a$10$.dKb4SWWiIMdpHYRD1UQ9.DJC.LkCyEAIj6lati8gbvUJYfA.fqEC','DOCTOR',2,NULL,1,1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `password`='$2a$10$.dKb4SWWiIMdpHYRD1UQ9.DJC.LkCyEAIj6lati8gbvUJYfA.fqEC', `role`='DOCTOR', `employee_id`=2, `patient_id`=NULL, `status`=1, `delmark`=1, `update_time`=NOW();

-- ---------- 医生B接诊的挂号(register id=10, employee_id=2，无 link) ----------
-- v2.0：register 无 patient_id 列；医生归属只认 register.employee_id。
INSERT INTO `register`
  (`id`,`case_number`,`real_name`,`gender`,`card_number`,`birthdate`,`age`,`age_type`,`home_address`,
   `visit_date`,`noon`,`deptment_id`,`employee_id`,`regist_level_id`,`settle_category_id`,`is_book`,`regist_method`,`regist_money`,`visit_state`)
VALUES
  (10,'BL_DB1','测试患者B','女','210102199505056789','1995-05-05',31,'岁','沈阳市测试地址B',
   '2026-06-11 09:30:00','上午',1,2,1,NULL,'否','现金',25.00,1)
ON DUPLICATE KEY UPDATE
  `employee_id`=VALUES(`employee_id`), `visit_date`=VALUES(`visit_date`), `case_number`=VALUES(`case_number`);

-- ---------- 医生B名下的处方/检查(用于跨医生 404 验证) ----------
INSERT INTO `prescription` (`id`,`register_id`,`drug_id`,`drug_usage`,`drug_number`,`creation_time`,`drug_state`)
VALUES (20,10,1,'医生B处方','10','2026-06-11 10:30:00','0')
ON DUPLICATE KEY UPDATE `register_id`=VALUES(`register_id`), `drug_usage`=VALUES(`drug_usage`), `drug_state`=VALUES(`drug_state`);

INSERT INTO `check_request`
  (`id`,`register_id`,`medical_technology_id`,`check_info`,`check_position`,`creation_time`,
   `check_employee_id`,`inputcheck_employee_id`,`check_time`,`check_result`,`check_state`,`check_remark`)
VALUES
  (20,10,1,'医生B检查','头部','2026-06-11 10:00:00',2,2,'2026-06-11 14:00:00','正常','0','医生B')
ON DUPLICATE KEY UPDATE `register_id`=VALUES(`register_id`), `check_info`=VALUES(`check_info`), `check_state`=VALUES(`check_state`);

-- ---------- AI read-side records for register ownership regression ----------
INSERT INTO `medical_record`
  (`id`,`register_id`,`readme`,`present`,`present_treat`,`history`,`allergy`,`physique`,`proposal`,`careful`,`diagnosis`,`cure`)
VALUES
  (10,10,'doctorB AI readme','doctorB present','doctorB present treat','doctorB history','doctorB allergy','doctorB physique','doctorB proposal','doctorB careful','doctorB diagnosis','doctorB cure')
ON DUPLICATE KEY UPDATE
  `register_id`=VALUES(`register_id`), `readme`=VALUES(`readme`), `diagnosis`=VALUES(`diagnosis`), `cure`=VALUES(`cure`);

INSERT INTO `medical_record_meta`
  (`id`,`medical_record_id`,`source`,`ai_request_snapshot`,`ai_result_snapshot`,`create_time`)
VALUES
  (1,1,'A','{"registerId":1}','{"source":"own"}',NOW()),
  (10,10,'A','{"registerId":10}','{"source":"other"}',NOW())
ON DUPLICATE KEY UPDATE
  `medical_record_id`=VALUES(`medical_record_id`), `source`=VALUES(`source`),
  `ai_request_snapshot`=VALUES(`ai_request_snapshot`), `ai_result_snapshot`=VALUES(`ai_result_snapshot`),
  `create_time`=VALUES(`create_time`);

INSERT INTO `prescription_audit_record`
  (`id`,`register_id`,`request_snapshot`,`result_json`,`risk_level`,`auditor_employee_id`,`creation_time`)
VALUES
  (1,1,'{"registerId":1}','{"riskLevel":"low"}','low',1,NOW()),
  (10,10,'{"registerId":10}','{"riskLevel":"high"}','high',2,NOW())
ON DUPLICATE KEY UPDATE
  `register_id`=VALUES(`register_id`), `request_snapshot`=VALUES(`request_snapshot`),
  `result_json`=VALUES(`result_json`), `risk_level`=VALUES(`risk_level`),
  `auditor_employee_id`=VALUES(`auditor_employee_id`), `creation_time`=VALUES(`creation_time`);
