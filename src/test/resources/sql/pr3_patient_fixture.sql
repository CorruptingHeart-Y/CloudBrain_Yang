-- ============================================================
-- 【仅本地验证 fixture — 禁止进入生产迁移路径，禁止由 docker compose 自动执行】
--
-- 用途：为患者门户端到端验证在本地隔离库 hospital_rbac_v11 中创建：
--   * patientB（patient.id=2）+ patient02 账号（PATIENT，patient_id=2，MD5 密码）
--   * patientA（patient.id=1，由 rbac_fixture.sql 建立，账号 patient01）的两条 register（id=1,2）
--   * patientB 的一条 register（id=3）
--   * 一条无 link 的历史异常 register（id=4，异常 card_number），验证不会被患者列表返回
--   * patient_register_link：patientA↔register 1,2；patientB↔register 3
--   * register.id=1（patientA 首诊）的病历/处方/检查/检验/处置各一项
--
-- v2.0：register 不再有 patient_id 列；患者归属经 patient_register_link 桥接表表达。
-- 前置：rbac_fixture.sql 已建立 department(1)/regist_level(1)/employee(1)/patient(1)/user_account(1,2,3)。
-- 幂等：INSERT ... ON DUPLICATE KEY UPDATE，重复执行重置状态。
-- ============================================================
USE `hospital_rbac_v11`;

-- ---------- patientB（患者主索引 id=2） ----------
INSERT INTO `patient` (`id`,`real_name`,`gender`,`card_number`,`birthdate`,`phone`,`home_address`,`delmark`,`create_time`,`update_time`)
VALUES (2,'测试患者B','女','210102199505056789','1995-05-05','13800000002','沈阳市测试地址B',1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `real_name`='测试患者B', `delmark`=1, `update_time`=NOW();

-- ---------- patient02 账号（PATIENT，patient_id=2，MD5 密码=patient123） ----------
INSERT INTO `user_account` (`id`,`username`,`password`,`role`,`employee_id`,`patient_id`,`status`,`delmark`,`create_time`,`update_time`)
VALUES (4,'patient02','c63f24079f1d5e4cae3fdc1a29116a7b','PATIENT',NULL,2,1,1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `password`='c63f24079f1d5e4cae3fdc1a29116a7b', `role`='PATIENT', `employee_id`=NULL, `patient_id`=2, `status`=1, `delmark`=1, `update_time`=NOW();

-- ---------- patientA 的两条 register（无 patient_id 列） ----------
INSERT INTO `register`
  (`id`,`case_number`,`real_name`,`gender`,`card_number`,`birthdate`,`age`,`age_type`,`home_address`,
   `visit_date`,`noon`,`deptment_id`,`employee_id`,`regist_level_id`,`settle_category_id`,`is_book`,`regist_method`,`regist_money`,`visit_state`)
VALUES
  (1,'BL_A1','测试患者','男','210102199001011234','1990-01-01',36,'岁','沈阳市测试地址',
   '2026-06-01 09:30:00','上午',1,1,1,NULL,'否','现金',25.00,1),
  (2,'BL_A2','测试患者','男','210102199001011234','1990-01-01',36,'岁','沈阳市测试地址',
   '2026-06-10 14:00:00','下午',1,1,1,NULL,'否','医保卡',25.00,3)
ON DUPLICATE KEY UPDATE
  `visit_date`=VALUES(`visit_date`), `visit_state`=VALUES(`visit_state`),
  `deptment_id`=VALUES(`deptment_id`), `employee_id`=VALUES(`employee_id`), `regist_level_id`=VALUES(`regist_level_id`),
  `case_number`=VALUES(`case_number`);

-- ---------- patientB 的一条 register ----------
INSERT INTO `register`
  (`id`,`case_number`,`real_name`,`gender`,`card_number`,`birthdate`,`age`,`age_type`,`home_address`,
   `visit_date`,`noon`,`deptment_id`,`employee_id`,`regist_level_id`,`settle_category_id`,`is_book`,`regist_method`,`regist_money`,`visit_state`)
VALUES
  (3,'BL_B1','测试患者B','女','210102199505056789','1995-05-05',31,'岁','沈阳市测试地址B',
   '2026-06-05 10:00:00','上午',1,1,1,NULL,'否','现金',25.00,2)
ON DUPLICATE KEY UPDATE
  `visit_date`=VALUES(`visit_date`), `visit_state`=VALUES(`visit_state`),
  `case_number`=VALUES(`case_number`);

-- ---------- 历史异常记录：异常 card_number（12 位），无 link，不应被任何患者列表返回 ----------
INSERT INTO `register`
  (`id`,`case_number`,`real_name`,`gender`,`card_number`,`birthdate`,`age`,`age_type`,`home_address`,
   `visit_date`,`noon`,`deptment_id`,`employee_id`,`regist_level_id`,`settle_category_id`,`is_book`,`regist_method`,`regist_money`,`visit_state`)
VALUES
  (4,'BL_NULL','历史患者','男','12位异常','1980-01-01',46,'岁',NULL,
   '2025-01-01 09:00:00','上午',1,1,1,NULL,'否','现金',10.00,3)
ON DUPLICATE KEY UPDATE
  `case_number`=VALUES(`case_number`), `visit_state`=VALUES(`visit_state`);

-- ---------- patient_register_link：患者↔挂号 桥接 ----------
INSERT INTO `patient_register_link` (`id`,`patient_id`,`register_id`,`link_source`,`create_time`) VALUES
  (1,1,1,'AUTO_INIT',NOW()),
  (2,1,2,'AUTO_INIT',NOW()),
  (3,2,3,'AUTO_INIT',NOW())
ON DUPLICATE KEY UPDATE `patient_id`=VALUES(`patient_id`), `link_source`=VALUES(`link_source`);
-- register 4 不建 link（异常 card_number，待人工）

-- ---------- register.id=1（patientA 首诊）的关联数据各一项 ----------
INSERT INTO `medical_record`
  (`id`,`register_id`,`readme`,`present`,`present_treat`,`history`,`allergy`,`physique`,`proposal`,`careful`,`diagnosis`,`cure`)
VALUES
  (1,1,'头痛三天','三天前起头痛','自行服药未缓解','高血压五年','青霉素过敏','神清，血压140/90','建议头颅CT','注意休息','偏头痛','对症止痛，随访')
ON DUPLICATE KEY UPDATE
  `readme`=VALUES(`readme`), `diagnosis`=VALUES(`diagnosis`), `cure`=VALUES(`cure`);

INSERT INTO `prescription`
  (`id`,`register_id`,`drug_id`,`drug_usage`,`drug_number`,`creation_time`,`drug_state`)
VALUES
  (1,1,1,'口服，一日三次','30','2026-06-01 10:30:00','0')
ON DUPLICATE KEY UPDATE
  `register_id`=VALUES(`register_id`), `drug_usage`=VALUES(`drug_usage`), `drug_state`=VALUES(`drug_state`);

INSERT INTO `check_request`
  (`id`,`register_id`,`medical_technology_id`,`check_info`,`check_position`,`creation_time`,
   `check_employee_id`,`inputcheck_employee_id`,`check_time`,`check_result`,`check_state`,`check_remark`)
VALUES
  (1,1,1,'胸部CT检查','胸部','2026-06-01 10:00:00',1,1,'2026-06-01 14:30:00','未见明显异常','1','配合良好')
ON DUPLICATE KEY UPDATE
  `register_id`=VALUES(`register_id`), `check_info`=VALUES(`check_info`), `check_state`=VALUES(`check_state`);

INSERT INTO `inspection_request`
  (`id`,`register_id`,`medical_technology_id`,`inspection_info`,`inspection_position`,`creation_time`,
   `inspection_employee_id`,`inputinspection_employee_id`,`inspection_time`,`inspection_result`,`inspection_state`,`inspection_remark`)
VALUES
  (1,1,1,'血常规检验','静脉血','2026-06-01 10:00:00',1,1,'2026-06-01 15:00:00','各项指标正常','1','空腹采血')
ON DUPLICATE KEY UPDATE
  `register_id`=VALUES(`register_id`), `inspection_info`=VALUES(`inspection_info`), `inspection_state`=VALUES(`inspection_state`);

INSERT INTO `disposal_request`
  (`id`,`register_id`,`medical_technology_id`,`disposal_info`,`disposal_position`,`creation_time`,
   `disposal_employee_id`,`inputdisposal_employee_id`,`disposal_time`,`disposal_result`,`disposal_state`,`disposal_remark`)
VALUES
  (1,1,1,'清创缝合术','左手前臂','2026-06-01 10:00:00',1,1,'2026-06-01 16:00:00','缝合完成，恢复良好','1','术后注意换药')
ON DUPLICATE KEY UPDATE
  `register_id`=VALUES(`register_id`), `disposal_info`=VALUES(`disposal_info`), `disposal_state`=VALUES(`disposal_state`);
