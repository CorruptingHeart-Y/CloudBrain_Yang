-- ============================================================
-- 种子数据 - hospital_cloud_brain
-- ============================================================
-- 用途：在清空后的库中重建一批语义自洽的演示数据，覆盖全部 23 张表。
--
-- 加密说明（关键）：
--   后端登录统一从 user_account 表查询，密码字段存储 BCrypt 哈希。
--   AuthServiceImpl 使用 new BCryptPasswordEncoder()（默认 strength=10）。
--   matchesPassword 同时兼容：60 位 BCrypt($2a/$2b/$2y) 与 32 位 MD5(hex)，
--   旧 MD5 首次登录自动升级为 BCrypt。
--   本脚本所有账号明文密码统一为 123456，存储其 BCrypt(10) 哈希：
--     $2b$10$7045RnXR8as4IkfivEcje.7x9iCSlAz95QxFzzPyX9L1Z3fJd96Qe
--   （$2b$ 前缀被 isBcrypt 接受，与 Spring BCryptPasswordEncoder 互通。）
--
-- 表关系概览（逻辑外键，均无物理约束）：
--   department ← employee.deptment_id / register.deptment_id / medical_technology.deptment_id
--   regist_level ← employee.regist_level_id / register.regist_level_id
--   scheduling ← employee.scheduling_id
--   settle_category ← register.settle_category_id（注：register 表无 patient_id 列）
--   employee ← register.employee_id / check_request.check_employee_id 等 / triage_record.operator_employee_id
--   patient ← user_account.patient_id（患者↔挂号归属经 patient_register_link 桥接，不依赖 register.patient_id）
--   user_account ← employee_id(ADMIN/DOCTOR) / patient_id(PATIENT)
--   register ← check_request/inspection_request/disposal_request/prescription/medical_record.register_id
--              / registration_ticket.register_id（抢号成功后回填）
--   medical_technology ← check/inspection/disposal_request.medical_technology_id
--   drug_info ← prescription.drug_id
--   medical_record ← medical_record_disease.medical_record_id / medical_record_meta.medical_record_id
--   disease ← medical_record_disease.disease_id
--   doctor_daily_quota ← employee_id（ADMIN 按医生+日期+午别放号；抢号/现场挂号扣 remaining）
--   registration_ticket ← patient_id / employee_id / regist_level_id / settle_category_id / register_id
--
-- 登录账号（明文密码均为 123456）：
--   admin       / ADMIN
--   zhangwei    / DOCTOR  (employee 1, 张伟,  神经内科)
--   lina        / DOCTOR  (employee 2, 李娜,  神经内科)
--   wangqiang   / DOCTOR  (employee 3, 王强,  心血管内科)
--   liuyang     / DOCTOR  (employee 4, 刘洋,  呼吸内科)
--   chenjing    / DOCTOR  (employee 5, 陈静,  消化内科)
--   zhaolei     / DOCTOR  (employee 6, 赵磊,  医学影像科-检查医师)
--   sunfang     / DOCTOR  (employee 7, 孙芳,  检验科-检验师)
--   patient01   / PATIENT (patient 1, 张三)
--   patient02   / PATIENT (patient 2, 李四)
--   patient03   / PATIENT (patient 3, 王五)
--   patient04   / PATIENT (patient 4, 赵六)
--   patient05   / PATIENT (patient 5, 钱七)
-- ============================================================

USE `hospital_cloud_brain`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 123456 的 BCrypt(10) 哈希，统一口令
SET @pw := '$2b$10$7045RnXR8as4IkfivEcje.7x9iCSlAz95QxFzzPyX9L1Z3fJd96Qe';


-- ============================================================
-- 1) 科室表 department
-- ============================================================
INSERT INTO `department` (`id`,`dept_code`,`dept_name`,`dept_type`,`delmark`) VALUES
(1,'SJNK','神经内科','临床科室',1),
(2,'XXGNK','心血管内科','临床科室',1),
(3,'HXNK','呼吸内科','临床科室',1),
(4,'XHNK','消化内科','临床科室',1),
(5,'YXYXK','医学影像科','医技科室',1),
(6,'JYK','检验科','医技科室',1);


-- ============================================================
-- 2) 挂号级别表 regist_level
-- ============================================================
INSERT INTO `regist_level` (`id`,`regist_code`,`regist_name`,`regist_fee`,`regist_quota`,`sequence_no`,`delmark`) VALUES
(1,'PT','普通号',10.00,100,1,1),
(2,'ZJ','专家号',30.00,50,2,1),
(3,'JZ','急诊号',15.00,30,3,1),
(4,'TX','特需号',80.00,10,4,1);


-- ============================================================
-- 3) 结算类别表 settle_category
-- ============================================================
INSERT INTO `settle_category` (`id`,`settle_code`,`settle_name`,`sequence_no`,`delmark`) VALUES
(1,'YB','医保',1,1),
(2,'ZF','自费',2,1),
(3,'XNH','新农合',3,1),
(4,'SYBX','商业保险',4,1);


-- ============================================================
-- 4) 排班表 scheduling
--    week_rule: 7 位 0/1，对应周一~周日
-- ============================================================
INSERT INTO `scheduling` (`id`,`rule_name`,`week_rule`,`delmark`) VALUES
(1,'周一至周五全天','1111100',1),
(2,'周一三五上午','1010100',1),
(3,'周二四下午','0101000',1);


-- ============================================================
-- 5) 员工表 employee（医生/医技人员）
--    password 存 BCrypt(123456)，与 user_account 一致；旧登录链路已废弃但保持字段一致
-- ============================================================
INSERT INTO `employee`
  (`id`,`deptment_id`,`regist_level_id`,`scheduling_id`,`realname`,`password`,`delmark`)
VALUES
(1,1,2,2,'张伟',@pw,1),
(2,1,1,1,'李娜',@pw,1),
(3,2,2,1,'王强',@pw,1),
(4,3,1,1,'刘洋',@pw,1),
(5,4,2,3,'陈静',@pw,1),
(6,5,NULL,1,'赵磊',@pw,1),   -- 医学影像科 检查医师
(7,6,NULL,1,'孙芳',@pw,1);   -- 检验科 检验师


-- ============================================================
-- 6) 患者身份主表 patient
--    card_number: 17 位数字 + 1 位(数字/X)，满足 ^[0-9]{17}[0-9Xx]$
-- ============================================================
INSERT INTO `patient`
  (`id`,`real_name`,`gender`,`card_number`,`birthdate`,`phone`,`home_address`,`delmark`,`create_time`,`update_time`)
VALUES
(1,'张三','男','210102199001011234','1990-01-01','13800000001','沈阳市和平区',1,NOW(),NOW()),
(2,'李四','女','210102199505052345','1995-05-05','13800000002','沈阳市沈河区',1,NOW(),NOW()),
(3,'王五','男','210103198812120012','1988-12-12','13800000003','沈阳市铁西区',1,NOW(),NOW()),
(4,'赵六','女','210104199207075678','1992-07-07','13800000004','沈阳市大东区',1,NOW(),NOW()),
(5,'钱七','男','21010520000101123X','2000-01-01','13800000005','沈阳市皇姑区',1,NOW(),NOW());


-- ============================================================
-- 7) 统一认证账号表 user_account
--    全部 password = BCrypt(123456)；status=1；token_version=1
-- ============================================================
INSERT INTO `user_account`
  (`id`,`username`,`password`,`role`,`employee_id`,`patient_id`,`status`,`token_version`,`delmark`,`create_time`,`update_time`)
VALUES
(1,'admin',     @pw,'ADMIN',  NULL,NULL,1,1,1,NOW(),NOW()),
(2,'zhangwei',  @pw,'DOCTOR', 1,   NULL,1,1,1,NOW(),NOW()),
(3,'lina',      @pw,'DOCTOR', 2,   NULL,1,1,1,NOW(),NOW()),
(4,'wangqiang', @pw,'DOCTOR', 3,   NULL,1,1,1,NOW(),NOW()),
(5,'liuyang',   @pw,'DOCTOR', 4,   NULL,1,1,1,NOW(),NOW()),
(6,'chenjing',  @pw,'DOCTOR', 5,   NULL,1,1,1,NOW(),NOW()),
(7,'zhaolei',   @pw,'DOCTOR', 6,   NULL,1,1,1,NOW(),NOW()),
(8,'sunfang',   @pw,'DOCTOR', 7,   NULL,1,1,1,NOW(),NOW()),
(9,'patient01', @pw,'PATIENT',NULL,1,   1,1,1,NOW(),NOW()),
(10,'patient02',@pw,'PATIENT',NULL,2,   1,1,1,NOW(),NOW()),
(11,'patient03',@pw,'PATIENT',NULL,3,   1,1,1,NOW(),NOW()),
(12,'patient04',@pw,'PATIENT',NULL,4,   1,1,1,NOW(),NOW()),
(13,'patient05',@pw,'PATIENT',NULL,5,   1,1,1,NOW(),NOW());


-- ============================================================
-- 8) 疾病字典 disease（含 ICD 编码与拼音助记码 disease_code）
-- ============================================================
INSERT INTO `disease` (`id`,`disease_code`,`disease_name`,`diseaseICD`,`disease_category`) VALUES
(1,'NGS','脑梗死','I63.9','神经系统疾病'),
(2,'PTT','偏头痛','G43.9','神经系统疾病'),
(3,'GXY','高血压病','I10','循环系统疾病'),
(4,'GZX','冠状动脉粥样硬化性心脏病','I25.1','循环系统疾病'),
(5,'SHXG','急性上呼吸道感染','J06.9','呼吸系统疾病'),
(6,'JXZQG','急性支气管炎','J20.9','呼吸系统疾病'),
(7,'MXWY','慢性胃炎','K29.5','消化系统疾病'),
(8,'XHKY','消化性溃疡','K27.9','消化系统疾病');


-- ============================================================
-- 9) 药品信息表 drug_info（mnemonic_code 为拼音助记码）
-- ============================================================
INSERT INTO `drug_info`
  (`id`,`drug_code`,`drug_name`,`drug_format`,`drug_unit`,`manufacturer`,`drug_dosage`,`drug_type`,`drug_price`,`mnemonic_code`,`creation_date`)
VALUES
(1,'ASP','阿司匹林肠溶片','100mg*30片','盒','拜耳医药','片剂','西药',25.00,'ASPLCRP',CURDATE()),
(2,'XBD','硝苯地平控释片','30mg*14片','盒','拜耳医药','片剂','西药',38.00,'XBDPKSP',CURDATE()),
(3,'AMX','阿莫西林胶囊','0.25g*24粒','盒','珠海联邦','胶囊','西药',15.00,'AXMLJN',CURDATE()),
(4,'AMLZ','奥美拉唑肠溶胶囊','20mg*14粒','盒','阿斯利康','胶囊','西药',45.00,'AMLZCRJN',CURDATE()),
(5,'ALDP','氨氯地平片','5mg*7片','盒','辉瑞制药','片剂','西药',32.00,'ALDP',CURDATE()),
(6,'BLF','布洛芬缓释胶囊','0.3g*20粒','盒','中美史克','胶囊','西药',22.00,'BLFHSJN',CURDATE()),
(7,'FFGC','复方甘草片','100片','瓶','广州白云山','片剂','中成药',12.00,'FFGCP',CURDATE()),
(8,'MTS','蒙脱石散','3g*15袋','盒','博福-益普生','散剂','西药',28.00,'MTSS',CURDATE());


-- ============================================================
-- 10) 医技项目表 medical_technology（检查/检验/处置）
--     tech_type: 检查 / 检验 / 处置；执行科室指向 department
-- ============================================================
INSERT INTO `medical_technology`
  (`id`,`tech_code`,`tech_name`,`tech_format`,`tech_price`,`tech_type`,`price_type`,`deptment_id`)
VALUES
(1,'JCH001','头部CT','次',280.00,'检查','医疗服务',5),
(2,'JCH002','胸部DR','次',180.00,'检查','医疗服务',5),
(3,'JCH003','经颅多普勒超声','次',220.00,'检查','医疗服务',5),
(4,'JY001','血常规','次',25.00,'检验','医疗服务',6),
(5,'JY002','尿常规','次',20.00,'检验','医疗服务',6),
(6,'JY003','肝功能','次',45.00,'检验','医疗服务',6),
(7,'JY004','血脂四项','次',50.00,'检验','医疗服务',6),
(8,'CZ001','静脉输液','次',30.00,'处置','医疗服务',4),
(9,'CZ002','雾化吸入','次',25.00,'处置','医疗服务',3),
(10,'CZ003','清创缝合','次',80.00,'处置','医疗服务',1);


-- ============================================================
-- 11) 患者历次挂号信息表 register
--     注意：本表无 patient_id 列；患者归属由 patient_register_link 表达
--     visit_state: 1-已挂号 2-医生接诊 3-看诊结束 4-已退号
-- ============================================================
INSERT INTO `register`
  (`id`,`case_number`,`real_name`,`gender`,`card_number`,`birthdate`,`age`,`age_type`,`home_address`,
   `visit_date`,`noon`,`deptment_id`,`employee_id`,`regist_level_id`,`settle_category_id`,
   `is_book`,`regist_method`,`regist_money`,`visit_state`)
VALUES
(1,'BL20260615001','张三','男','210102199001011234','1990-01-01',36,'年','沈阳市和平区',
   '2026-06-15 09:00:00','上午',1,1,2,1,'否','医保卡',30.00,3),
(2,'BL20260620001','张三','男','210102199001011234','1990-01-01',36,'年','沈阳市和平区',
   '2026-06-20 10:00:00','上午',2,3,2,1,'是','医保卡',30.00,3),
(3,'BL20260618001','李四','女','210102199505052345','1995-05-05',31,'年','沈阳市沈河区',
   '2026-06-18 14:00:00','下午',3,4,1,2,'否','微信',10.00,3),
(4,'BL20260701001','王五','男','210103198812120012','1988-12-12',37,'年','沈阳市铁西区',
   '2026-07-01 09:30:00','上午',1,1,2,1,'是','医保卡',30.00,2),
(5,'BL20260625001','赵六','女','210104199207075678','1992-07-07',34,'年','沈阳市大东区',
   '2026-06-25 10:30:00','上午',4,5,2,3,'否','支付宝',30.00,3),
(6,'BL20260703001','钱七','男','21010520000101123X','2000-01-01',26,'年','沈阳市皇姑区',
   '2026-07-03 08:30:00','上午',1,2,1,2,'否','现金',10.00,1),
(7,'BL20260610001','李四','女','210102199505052345','1995-05-05',31,'年','沈阳市沈河区',
   '2026-06-10 09:00:00','上午',1,2,1,1,'否','医保卡',10.00,4);


-- ============================================================
-- 12) 患者挂号关联桥接表 patient_register_link
--     表达 患者 ↔ 挂号 归属（register 无 patient_id 列，由此表桥接）
-- ============================================================
INSERT INTO `patient_register_link` (`patient_id`,`register_id`,`link_source`,`create_time`) VALUES
(1,1,'AUTO_INIT',NOW()),
(1,2,'AUTO_INIT',NOW()),
(2,3,'AUTO_INIT',NOW()),
(3,4,'AUTO_INIT',NOW()),
(4,5,'AUTO_INIT',NOW()),
(5,6,'AUTO_INIT',NOW()),
(2,7,'AUTO_INIT',NOW());


-- ============================================================
-- 13) 患者病历表 medical_record（按挂号 upsert，uk_register_id）
--     source: A=AI草稿生成  M=人工录入
-- ============================================================
INSERT INTO `medical_record`
  (`id`,`register_id`,`readme`,`present`,`present_treat`,`history`,`allergy`,`physique`,
   `proposal`,`careful`,`diagnosis`,`cure`,`source`)
VALUES
(1,1,'头痛伴头晕三天','三天前无明显诱因出现持续性胀痛，伴头晕，无恶心呕吐','未规律治疗','高血压病史3年','否认药物过敏','神清，双侧瞳孔等大等圆，四肢肌力正常',
   '建议头部CT、血脂检查','低盐低脂饮食，规律服药','脑梗死','改善循环、抗血小板聚集治疗','M'),
(2,2,'头晕、头胀不适一周','近一周反复头晕、头胀，测血压偏高，无胸闷心悸','未服药','高血压病史3年','否认','血压160/95mmHg，心率78次/分',
   '建议血脂、心电图检查','监测血压，低盐饮食','高血压病','降压治疗，氨氯地平口服','A'),
(3,3,'咳嗽、咳痰五天','五天前受凉后出现咳嗽，咳白痰，伴咽痛，无明显发热','自行口服感冒药效果不佳','体健','否认','咽部充血，双肺呼吸音粗',
   '建议血常规、胸部DR检查','多饮水，注意保暖','急性上呼吸道感染','对症抗感染治疗','M'),
(4,5,'上腹部隐痛反复发作半月','半月来上腹部隐痛，餐后明显，伴反酸嗳气，无黑便','间断口服胃药','否认慢性病史','否认','腹软，上腹轻压痛',
   '建议幽门螺杆菌检测、肝功能检查','清淡饮食，忌辛辣','慢性胃炎','抑酸、护胃治疗','A');


-- ============================================================
-- 14) 病历首页疾病关联表 medical_record_disease
-- ============================================================
INSERT INTO `medical_record_disease` (`medical_record_id`,`disease_id`) VALUES
(1,1),  -- 脑梗死
(1,3),  -- 合并高血压病
(2,3),  -- 高血压病
(3,5),  -- 急性上呼吸道感染
(4,7);  -- 慢性胃炎


-- ============================================================
-- 15) 病历元数据表 medical_record_meta（仅 source=A 的病历记录来源快照）
-- ============================================================
INSERT INTO `medical_record_meta`
  (`medical_record_id`,`source`,`ai_request_snapshot`,`ai_result_snapshot`,`create_time`)
VALUES
(2,'A',
 '{"registerId":2,"dialogue":"头晕头胀一周，测血压偏高，无胸闷心悸"}',
 '{"readme":"头晕、头胀不适一周","diagnosis":"高血压病","cure":"降压治疗，氨氯地平口服"}',
 NOW()),
(4,'A',
 '{"registerId":5,"dialogue":"上腹部隐痛反复半月，餐后明显，伴反酸嗳气"}',
 '{"readme":"上腹部隐痛反复发作半月","diagnosis":"慢性胃炎","cure":"抑酸、护胃治疗"}',
 NOW());


-- ============================================================
-- 16) 处方表 prescription
--     drug_state: 已发药 / 待发药
-- ============================================================
INSERT INTO `prescription`
  (`id`,`register_id`,`drug_id`,`drug_usage`,`drug_number`,`creation_time`,`drug_state`)
VALUES
(1,1,1,'口服，一日一次，一次1片','30','2026-06-15 09:20:00','已发药'),
(2,1,5,'口服，一日一次，一次1片','7', '2026-06-15 09:20:00','已发药'),
(3,2,5,'口服，一日一次，一次1片','7', '2026-06-20 10:15:00','已发药'),
(4,3,3,'口服，一日三次，一次2粒','24','2026-06-18 14:15:00','已发药'),
(5,5,4,'口服，一日一次，一次1粒','14','2026-06-25 10:40:00','待发药');


-- ============================================================
-- 17) 检查申请表 check_request
--     check_state: 已完成 / 待检
-- ============================================================
INSERT INTO `check_request`
  (`id`,`register_id`,`medical_technology_id`,`check_info`,`check_position`,`creation_time`,
   `check_employee_id`,`inputcheck_employee_id`,`check_time`,`check_result`,`check_state`,`check_remark`)
VALUES
(1,1,1,'头部CT检查','头部','2026-06-15 09:25:00',
   6,6,'2026-06-15 11:00:00','未见明显异常','已完成',''),
(2,3,2,'胸部DR检查','胸部','2026-06-18 14:20:00',
   6,6,'2026-06-18 15:30:00','双肺纹理增多','已完成','');


-- ============================================================
-- 18) 检验申请表 inspection_request
-- ============================================================
INSERT INTO `inspection_request`
  (`id`,`register_id`,`medical_technology_id`,`inspection_info`,`inspection_position`,`creation_time`,
   `inspection_employee_id`,`inputinspection_employee_id`,`inspection_time`,`inspection_result`,`inspection_state`,`inspection_remark`)
VALUES
(1,1,4,'血常规','静脉血','2026-06-15 09:25:00',
   7,7,'2026-06-15 11:30:00','各项指标正常','已完成',''),
(2,1,7,'血脂四项','静脉血','2026-06-15 09:25:00',
   7,7,'2026-06-15 11:30:00','总胆固醇偏高','已完成','低脂饮食'),
(3,3,4,'血常规','静脉血','2026-06-18 14:20:00',
   7,7,'2026-06-18 15:30:00','白细胞计数偏高','已完成',''),
(4,5,6,'肝功能','静脉血','2026-06-25 10:40:00',
   7,7,'2026-06-25 11:00:00','各项指标正常','已完成','');


-- ============================================================
-- 19) 处置申请表 disposal_request
-- ============================================================
INSERT INTO `disposal_request`
  (`id`,`register_id`,`medical_technology_id`,`disposal_info`,`disposal_position`,`creation_time`,
   `disposal_employee_id`,`inputdisposal_employee_id`,`disposal_time`,`disposal_result`,`disposal_state`,`disposal_remark`)
VALUES
(1,3,9,'雾化吸入','呼吸道','2026-06-18 14:25:00',
   4,4,'2026-06-18 16:00:00','雾化完成，咳嗽减轻','已完成',''),
(2,5,8,'静脉输液','静脉','2026-06-25 10:45:00',
   5,5,'2026-06-25 14:00:00','输液完成，无不适','已完成','');


-- ============================================================
-- 20) 诊前分诊记录表 triage_record（AI 留痕）
-- ============================================================
INSERT INTO `triage_record`
  (`card_number`,`case_number`,`patient_name`,`gender`,`age`,`chief_complaint`,
   `recommend_dept_ids`,`recommend_doctor_ids`,`ai_raw_result`,`operator_employee_id`,`creation_time`)
VALUES
('210102199001011234','BL20260615001','张三','男',36,'头痛伴头晕三天',
 '1','1','{"reason":"头痛伴神经系统症状，建议神经内科就诊","score":0.92}',1,'2026-06-15 08:50:00'),
('210102199505052345','BL20260618001','李四','女',31,'咳嗽咳痰五天',
 '3','4','{"reason":"咳嗽咳痰，建议呼吸内科就诊","score":0.88}',2,'2026-06-18 13:50:00'),
('210104199207075678','BL20260625001','赵六','女',34,'上腹部隐痛反复发作',
 '4','5','{"reason":"上腹部疼痛餐后加重，建议消化内科就诊","score":0.85}',5,'2026-06-25 10:20:00');


-- ============================================================
-- 21) 处方审核记录表 prescription_audit_record（AI 留痕）
-- ============================================================
INSERT INTO `prescription_audit_record`
  (`register_id`,`request_snapshot`,`result_json`,`risk_level`,`auditor_employee_id`,`creation_time`)
VALUES
(1,
 '{"patient":"张三","drugs":[{"name":"阿司匹林肠溶片","usage":"一日一次"},{"name":"氨氯地平片","usage":"一日一次"}]}',
 '{"risk_level":"low","suggestions":[],"interactions":[],"riskItems":[]}',
 'low',1,'2026-06-15 09:30:00'),
(3,
 '{"patient":"李四","drugs":[{"name":"阿莫西林胶囊","usage":"一日三次"}]}',
 '{"risk_level":"low","suggestions":["注意青霉素过敏史"],"interactions":[],"riskItems":[]}',
 'low',4,'2026-06-18 14:20:00');


-- ============================================================
-- 22) 医生每日号源表 doctor_daily_quota（ADMIN 放号）
--     仅门诊医生 employee 1-5 放号；6/7 为医技岗不发门诊号。
--     capacity 按医生挂号级别设定：专家号(2)=20  普通号(1)=40
--     remaining：07-03 上午 emp2 已被钱七抢 1 → 39；emp3 上午约满 → 0
--     日期覆盖今天(2026-07-03)及未来两天，供患者抢号页下拉。
--     delmark/create_time/update_time 走表默认值(1 / NOW / NOW)，省略。
-- ============================================================
INSERT INTO `doctor_daily_quota`
  (`employee_id`,`quota_date`,`noon`,`capacity`,`remaining`)
VALUES
-- 2026-07-03 上午
(1,'2026-07-03','上午',20,20),   -- 张伟(专家号)：满号
(2,'2026-07-03','上午',40,39),   -- 李娜(普通号)：钱七抢号成功扣1
(3,'2026-07-03','上午',20,0),    -- 王强(专家号)：约满，李四抢号失败
(4,'2026-07-03','上午',40,40),
(5,'2026-07-03','上午',20,20),
-- 2026-07-03 下午
(1,'2026-07-03','下午',20,20),
(2,'2026-07-03','下午',40,40),
(3,'2026-07-03','下午',20,20),
(4,'2026-07-03','下午',40,40),
(5,'2026-07-03','下午',20,20),
-- 2026-07-04 上午
(1,'2026-07-04','上午',20,20),   -- 张三 PENDING 票据在途(未扣号)
(2,'2026-07-04','上午',40,40),
(3,'2026-07-04','上午',20,20),
(4,'2026-07-04','上午',40,40),
(5,'2026-07-04','上午',20,20),
-- 2026-07-04 下午
(1,'2026-07-04','下午',20,20),
(2,'2026-07-04','下午',40,40),
(3,'2026-07-04','下午',20,20),
(4,'2026-07-04','下午',40,40),
(5,'2026-07-04','下午',20,20),
-- 2026-07-05 上午
(1,'2026-07-05','上午',20,20),
(2,'2026-07-05','上午',40,40),
(3,'2026-07-05','上午',20,20),
(4,'2026-07-05','上午',40,40),
(5,'2026-07-05','上午',20,20),
-- 2026-07-05 下午
(1,'2026-07-05','下午',20,20),
(2,'2026-07-05','下午',40,40),
(3,'2026-07-05','下午',20,20),
(4,'2026-07-05','下午',40,40),
(5,'2026-07-05','下午',20,20);


-- ============================================================
-- 23) 抢号票据表 registration_ticket（异步链路状态机留痕）
--     幂等 uk_patient_date_noon：每患者每半日仅一票(任意状态)。
--     ticket_no 格式：QH + yyyyMMddHHmmss + 3位随机。
--     三种状态各一例，演示 PENDING→SUCCESS/FAILED 流转：
--       ① 钱七 SUCCESS → register 6（与第11节 register id=6 / 第12节 link 一致）
--       ② 张三 PENDING → 在途，尚无 register_id（emp1 07-04 上午号源充足）
--       ③ 李四 FAILED  → emp3 07-03 上午约满，fail_reason 记录原因
-- ============================================================
INSERT INTO `registration_ticket`
  (`ticket_no`,`patient_id`,`employee_id`,`visit_date`,`noon`,`regist_level_id`,`settle_category_id`,
   `status`,`register_id`,`fail_reason`,`create_time`)
VALUES
('QH20260703083001047',5,2,'2026-07-03','上午',1,2,
   'SUCCESS',6,NULL,'2026-07-03 08:30:01'),
('QH20260703091500023',1,1,'2026-07-04','上午',2,1,
   'PENDING',NULL,NULL,'2026-07-03 09:15:00'),
('QH20260703084500112',2,3,'2026-07-03','上午',2,2,
   'FAILED',NULL,'号源已约满','2026-07-03 08:45:00');


SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 只读校验：各表行数
-- ============================================================
SELECT 'department' AS t, COUNT(*) AS cnt FROM `department`
UNION ALL SELECT 'regist_level', COUNT(*) FROM `regist_level`
UNION ALL SELECT 'settle_category', COUNT(*) FROM `settle_category`
UNION ALL SELECT 'scheduling', COUNT(*) FROM `scheduling`
UNION ALL SELECT 'employee', COUNT(*) FROM `employee`
UNION ALL SELECT 'patient', COUNT(*) FROM `patient`
UNION ALL SELECT 'user_account', COUNT(*) FROM `user_account`
UNION ALL SELECT 'register', COUNT(*) FROM `register`
UNION ALL SELECT 'patient_register_link', COUNT(*) FROM `patient_register_link`
UNION ALL SELECT 'medical_record', COUNT(*) FROM `medical_record`
UNION ALL SELECT 'medical_record_disease', COUNT(*) FROM `medical_record_disease`
UNION ALL SELECT 'medical_record_meta', COUNT(*) FROM `medical_record_meta`
UNION ALL SELECT 'prescription', COUNT(*) FROM `prescription`
UNION ALL SELECT 'check_request', COUNT(*) FROM `check_request`
UNION ALL SELECT 'inspection_request', COUNT(*) FROM `inspection_request`
UNION ALL SELECT 'disposal_request', COUNT(*) FROM `disposal_request`
UNION ALL SELECT 'disease', COUNT(*) FROM `disease`
UNION ALL SELECT 'drug_info', COUNT(*) FROM `drug_info`
UNION ALL SELECT 'medical_technology', COUNT(*) FROM `medical_technology`
UNION ALL SELECT 'triage_record', COUNT(*) FROM `triage_record`
UNION ALL SELECT 'prescription_audit_record', COUNT(*) FROM `prescription_audit_record`
UNION ALL SELECT 'doctor_daily_quota', COUNT(*) FROM `doctor_daily_quota`
UNION ALL SELECT 'registration_ticket', COUNT(*) FROM `registration_ticket`;

-- 账号清单（明文密码均为 123456）
SELECT `id`,`username`,`role`,
       CASE WHEN `employee_id` IS NOT NULL THEN `employee_id` ELSE `patient_id` END AS `bind_id`,
       `status`
FROM `user_account` ORDER BY `id`;
