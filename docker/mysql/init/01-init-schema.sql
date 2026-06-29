-- ============================================================
-- 东软智慧云脑诊疗平台 - 数据库建库建表脚本
-- 核心业务表与字典表 MySQL 建表语句 (共 15 张表)
-- ============================================================
-- 说明：
--   本脚本包含东软智慧云脑诊疗平台全部 15 张核心表。
--   为满足高性能、高可扩展性的企业级前后端分离架构要求，
--   所有物理外键均已转为逻辑外键，并针对高频关联查询和
--   筛选字段（如编码、姓名、时间、状态、拼音助记码等）
--   设计了合理的单列与复合索引。
-- ============================================================

-- -----------------------------------------------------------
-- 创建数据库
-- -----------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `hospital_cloud_brain`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `hospital_cloud_brain`;

-- -----------------------------------------------------------
-- (1)  科室表 (department)
-- 功能：保存医生的出诊科室分类信息，支持编码唯一校验及名称检索。
-- -----------------------------------------------------------
CREATE TABLE `department` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `dept_code` VARCHAR(64) DEFAULT NULL COMMENT '科室编码，如：SJNK',
  `dept_name` VARCHAR(64) DEFAULT NULL COMMENT '科室名称，如：神经内科',
  `dept_type` VARCHAR(64) DEFAULT NULL COMMENT '科室类型',
  `delmark` INT(1) DEFAULT 1 COMMENT '生效标记：1-正常 0-已删除',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_dept_code` (`dept_code`),
  INDEX `idx_dept_name` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

-- -----------------------------------------------------------
-- (2)  挂号级别表 (regist_level)
-- 功能：存储挂号别名、限额、挂号费等字典数据。
-- -----------------------------------------------------------
CREATE TABLE `regist_level` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `regist_code` VARCHAR(64) DEFAULT NULL COMMENT '号别编码',
  `regist_name` VARCHAR(64) DEFAULT NULL COMMENT '号别名称',
  `regist_fee` DECIMAL(8,2) DEFAULT NULL COMMENT '挂号费',
  `regist_quota` INT(5) DEFAULT NULL COMMENT '挂号限额',
  `sequence_no` INT(3) DEFAULT NULL COMMENT '显示顺序号',
  `delmark` INT(1) DEFAULT 1 COMMENT '生效标记：1-正常 0-已删除',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_regist_code` (`regist_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号级别表';

-- -----------------------------------------------------------
-- (3)  结算类别表 (settle_category)
-- 功能：存放医保、自费、新农合等患者结算类别。
-- -----------------------------------------------------------
CREATE TABLE `settle_category` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `settle_code` VARCHAR(64) DEFAULT NULL COMMENT '类别编码',
  `settle_name` VARCHAR(64) DEFAULT NULL COMMENT '类别名称',
  `sequence_no` INT(3) DEFAULT NULL COMMENT '显示顺序号',
  `delmark` INT(1) DEFAULT 1 COMMENT '生效标记：1-正常 0-已删除',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_settle_code` (`settle_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算类别表';

-- -----------------------------------------------------------
-- (4)  排班表 (scheduling)
-- 功能：记录医院医生的出诊排班规则。
-- -----------------------------------------------------------
CREATE TABLE `scheduling` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `rule_name` VARCHAR(64) DEFAULT NULL COMMENT '排班规则名称',
  `week_rule` VARCHAR(14) DEFAULT NULL COMMENT '星期规则',
  `delmark` INT(1) DEFAULT 1 COMMENT '生效标记：1-正常 0-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班表';

-- -----------------------------------------------------------
-- (5)  医院员工表 (employee)
-- 功能：保存医生等员工账户基础信息，建立科室与排班逻辑索引。
-- -----------------------------------------------------------
CREATE TABLE `employee` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `deptment_id` INT(9) DEFAULT NULL COMMENT '所在科室id，指向department(id)',
  `regist_level_id` INT(9) DEFAULT NULL COMMENT '挂号级别id，指向regist_level(ID)',
  `scheduling_id` INT(9) DEFAULT NULL COMMENT '排班id',
  `realname` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `password` VARCHAR(64) DEFAULT NULL COMMENT '密码',
  `delmark` INT(1) DEFAULT 1 COMMENT '生效标记：1-生效 0-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_deptment_id` (`deptment_id`),
  INDEX `idx_realname` (`realname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院员工表';

-- -----------------------------------------------------------
-- (6)  患者历次挂号信息表 (register)
-- 功能：核心流程表，记录患者每次就诊挂号详情，配置多高频复杂查询索引。
-- -----------------------------------------------------------
CREATE TABLE `register` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `case_number` VARCHAR(64) DEFAULT NULL COMMENT '病历号',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '姓名',
  `gender` VARCHAR(6) DEFAULT NULL COMMENT '性别：男/女',
  `card_number` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `birthdate` DATE DEFAULT NULL COMMENT '出生日期',
  `age` INT(3) DEFAULT NULL COMMENT '年龄',
  `age_type` VARCHAR(6) DEFAULT NULL COMMENT '年龄类型：年/天',
  `home_address` VARCHAR(64) DEFAULT NULL COMMENT '家庭住址',
  `visit_date` DATETIME DEFAULT NULL COMMENT '本次看诊日期',
  `noon` VARCHAR(6) DEFAULT NULL COMMENT '午别：上午/下午',
  `deptment_id` INT(9) DEFAULT NULL COMMENT '本次挂号科室ID，指向department(ID)',
  `employee_id` INT(9) DEFAULT NULL COMMENT '本次挂号医生ID，指向employee(ID)',
  `regist_level_id` INT(9) DEFAULT NULL COMMENT '本次挂号级别ID，指向regist_level(ID)',
  `settle_category_id` INT(9) DEFAULT NULL COMMENT '结算类别ID，指向settle_category(ID)',
  `is_book` CHAR(2) DEFAULT NULL COMMENT '病历本要否：是/否',
  `regist_method` VARCHAR(10) DEFAULT NULL COMMENT '收费方式：现金、银行卡、医保卡、微信、支付宝',
  `regist_money` DECIMAL(8,2) DEFAULT NULL COMMENT '挂号金额',
  `visit_state` INT(1) DEFAULT NULL COMMENT '本次看诊状态：1-已挂号 2-医生接诊 3-看诊结束 4-已退号',
  PRIMARY KEY (`id`),
  INDEX `idx_case_number` (`case_number`),
  INDEX `idx_card_number` (`card_number`),
  INDEX `idx_visit_date` (`visit_date`),
  INDEX `idx_deptment_id` (`deptment_id`),
  INDEX `idx_employee_id` (`employee_id`),
  INDEX `idx_visit_state` (`visit_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者历次挂号信息表';

-- -----------------------------------------------------------
-- (7)  检查申请表 (check_request)
-- 功能：开立与录入大型医技成像检查（如CT、超声等）结果及状态。
-- -----------------------------------------------------------
CREATE TABLE `check_request` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `register_id` INT(9) DEFAULT NULL COMMENT '挂号id，指向register(ID)',
  `medical_technology_id` INT(9) DEFAULT NULL COMMENT '医技项目id',
  `check_info` VARCHAR(512) DEFAULT NULL COMMENT '目的要求',
  `check_position` VARCHAR(255) DEFAULT NULL COMMENT '检查部位',
  `creation_time` DATETIME DEFAULT NULL COMMENT '开立时间',
  `check_employee_id` INT(9) DEFAULT NULL COMMENT '检查人员id',
  `inputcheck_employee_id` INT(9) DEFAULT NULL COMMENT '结果录入人员id',
  `check_time` DATETIME DEFAULT NULL COMMENT '检查时间',
  `check_result` VARCHAR(512) DEFAULT NULL COMMENT '检查结果',
  `check_state` VARCHAR(64) DEFAULT NULL COMMENT '状态',
  `check_remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  INDEX `idx_register_id` (`register_id`),
  INDEX `idx_creation_time` (`creation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查申请表';

-- -----------------------------------------------------------
-- (8)  检验申请表 (inspection_request)
-- 功能：存储化验样本检验流转结果信息。
-- -----------------------------------------------------------
CREATE TABLE `inspection_request` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `register_id` INT(9) DEFAULT NULL COMMENT '挂号id，指向register(ID)',
  `medical_technology_id` INT(9) DEFAULT NULL COMMENT '医技项目id',
  `inspection_info` VARCHAR(512) DEFAULT NULL COMMENT '目的要求',
  `inspection_position` VARCHAR(255) DEFAULT NULL COMMENT '检验部位',
  `creation_time` DATETIME DEFAULT NULL COMMENT '开立时间',
  `inspection_employee_id` INT(9) DEFAULT NULL COMMENT '检验人员id',
  `inputinspection_employee_id` INT(9) DEFAULT NULL COMMENT '结果录入人员id',
  `inspection_time` DATETIME DEFAULT NULL COMMENT '检验时间',
  `inspection_result` VARCHAR(512) DEFAULT NULL COMMENT '检验结果',
  `inspection_state` VARCHAR(64) DEFAULT NULL COMMENT '状态',
  `inspection_remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  INDEX `idx_register_id` (`register_id`),
  INDEX `idx_creation_time` (`creation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验申请表';

-- -----------------------------------------------------------
-- (9)  处置申请表 (disposal_request)
-- 功能：记录非检查检验类的临床治疗处置明细与执行情况。
-- -----------------------------------------------------------
CREATE TABLE `disposal_request` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型主键',
  `register_id` INT(9) DEFAULT NULL COMMENT '挂号id，指向register(ID)',
  `medical_technology_id` INT(9) DEFAULT NULL COMMENT '医技项目id',
  `disposal_info` VARCHAR(512) DEFAULT NULL COMMENT '目的要求',
  `disposal_position` VARCHAR(255) DEFAULT NULL COMMENT '处置部位',
  `creation_time` DATETIME DEFAULT NULL COMMENT '开立时间',
  `disposal_employee_id` INT(9) DEFAULT NULL COMMENT '处置人员id',
  `inputdisposal_employee_id` INT(9) DEFAULT NULL COMMENT '结果录入人员id',
  `disposal_time` DATETIME DEFAULT NULL COMMENT '处置时间',
  `disposal_result` VARCHAR(512) DEFAULT NULL COMMENT '处置结果',
  `disposal_state` VARCHAR(64) DEFAULT NULL COMMENT '状态',
  `disposal_remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  INDEX `idx_register_id` (`register_id`),
  INDEX `idx_creation_time` (`creation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处置申请表';

-- -----------------------------------------------------------
-- (10) 医技项目表 (medical_technology)
-- 功能：维护医院提供的检查、检验、处置等项目基础收费与科室字典。
-- -----------------------------------------------------------
CREATE TABLE `medical_technology` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `tech_code` VARCHAR(64) DEFAULT NULL COMMENT '项目编码',
  `tech_name` VARCHAR(64) DEFAULT NULL COMMENT '项目名称',
  `tech_format` VARCHAR(64) DEFAULT NULL COMMENT '规格',
  `tech_price` DECIMAL(8,2) DEFAULT NULL COMMENT '单价',
  `tech_type` VARCHAR(64) DEFAULT NULL COMMENT '类型',
  `price_type` VARCHAR(64) DEFAULT NULL COMMENT '费用分类',
  `deptment_id` INT(9) DEFAULT NULL COMMENT '执行科室',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_tech_code` (`tech_code`),
  INDEX `idx_tech_name` (`tech_name`),
  INDEX `idx_deptment_id` (`deptment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医技项目表';

-- -----------------------------------------------------------
-- (11) 患者病历表 (medical_record)
-- 功能：AI原生模块落库核心表。用于承接并存储由大语言模型对话
--       转换生成的结构化门诊病历字段。
-- -----------------------------------------------------------
CREATE TABLE `medical_record` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `register_id` INT(9) DEFAULT NULL COMMENT '挂号ID，外键',
  `readme` VARCHAR(512) DEFAULT NULL COMMENT '主诉',
  `present` VARCHAR(512) DEFAULT NULL COMMENT '现病史',
  `present_treat` VARCHAR(512) DEFAULT NULL COMMENT '现病治疗情况',
  `history` VARCHAR(512) DEFAULT NULL COMMENT '既往史',
  `allergy` VARCHAR(512) DEFAULT NULL COMMENT '过敏史',
  `physique` VARCHAR(512) DEFAULT NULL COMMENT '体格检查',
  `proposal` VARCHAR(512) DEFAULT NULL COMMENT '检查/检验建议',
  `careful` VARCHAR(512) DEFAULT NULL COMMENT '注意事项',
  `diagnosis` VARCHAR(512) DEFAULT NULL COMMENT '诊断结果',
  `cure` VARCHAR(512) DEFAULT NULL COMMENT '处理意见',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_register_id` (`register_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者病历表';

-- -----------------------------------------------------------
-- (12) 病历首页疾病关联表 (medical_record_disease)
-- 功能：病历与疾病的多对多关联表，支持对同一病历去重关联索引。
-- -----------------------------------------------------------
CREATE TABLE `medical_record_disease` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长主键',
  `medical_record_id` INT(9) DEFAULT NULL COMMENT '病历id',
  `disease_id` INT(9) DEFAULT NULL COMMENT '疾病id',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_record_disease` (`medical_record_id`, `disease_id`),
  INDEX `idx_disease_id` (`disease_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病历首页疾病关联表';

-- -----------------------------------------------------------
-- (13) 疾病表 (disease)
-- 功能：存储ICD国际编码以及疾病拼音助记码，方便医生端高频检索秒级回显。
-- -----------------------------------------------------------
CREATE TABLE `disease` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `disease_code` VARCHAR(64) DEFAULT NULL COMMENT '疾病助记编码',
  `disease_name` VARCHAR(255) DEFAULT NULL COMMENT '疾病名称',
  `diseaseICD` VARCHAR(64) DEFAULT NULL COMMENT '国际ICD编码',
  `disease_category` VARCHAR(64) DEFAULT NULL COMMENT '疾病所属分类',
  PRIMARY KEY (`id`),
  INDEX `idx_disease_code` (`disease_code`),
  INDEX `idx_diseaseICD` (`diseaseICD`),
  INDEX `idx_disease_name` (`disease_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='疾病表';

-- -----------------------------------------------------------
-- (14) 处方表 (prescription)
-- 功能：记录门诊医生开立的药方明细及用法用量，
--       为AI审核安全用药提供基础支撑。
-- -----------------------------------------------------------
CREATE TABLE `prescription` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `register_id` INT(9) DEFAULT NULL COMMENT '病历id',
  `drug_id` INT(9) DEFAULT NULL COMMENT '药品id',
  `drug_usage` VARCHAR(255) DEFAULT NULL COMMENT '用法用量频次',
  `drug_number` VARCHAR(255) DEFAULT NULL COMMENT '数量',
  `creation_time` DATETIME DEFAULT NULL COMMENT '开立时间',
  `drug_state` VARCHAR(64) DEFAULT NULL COMMENT '状态',
  PRIMARY KEY (`id`),
  INDEX `idx_register_id` (`register_id`),
  INDEX `idx_drug_id` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方表';

-- -----------------------------------------------------------
-- (15) 药品信息表 (drug_info)
-- 功能：存储全部院内可用药品基本规格、厂家、单价和拼音快速查询助记码。
-- -----------------------------------------------------------
CREATE TABLE `drug_info` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `drug_code` VARCHAR(255) DEFAULT NULL COMMENT '药品编码',
  `drug_name` VARCHAR(255) DEFAULT NULL COMMENT '药品名称',
  `drug_format` VARCHAR(255) DEFAULT NULL COMMENT '药品规格',
  `drug_unit` VARCHAR(16) DEFAULT NULL COMMENT '包装单位',
  `manufacturer` VARCHAR(255) DEFAULT NULL COMMENT '生产厂家',
  `drug_dosage` VARCHAR(64) DEFAULT NULL COMMENT '药剂类型',
  `drug_type` VARCHAR(64) DEFAULT NULL COMMENT '药品类型',
  `drug_price` DECIMAL(8,2) DEFAULT NULL COMMENT '药品单价',
  `mnemonic_code` VARCHAR(255) DEFAULT NULL COMMENT '拼音助记码',
  `creation_date` DATE DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_drug_code` (`drug_code`),
  INDEX `idx_mnemonic_code` (`mnemonic_code`),
  INDEX `idx_drug_name` (`drug_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品信息表';

-- ============================================================
-- 建表完成，共 15 张表
-- 字典表：department / regist_level / settle_category / scheduling / medical_technology / disease / drug_info
-- 业务表：employee / register / check_request / inspection_request / disposal_request / medical_record / medical_record_disease / prescription
-- ============================================================
