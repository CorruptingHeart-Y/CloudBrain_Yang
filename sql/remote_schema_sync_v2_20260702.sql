



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


CREATE TABLE IF NOT EXISTS `medical_record_meta` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `medical_record_id` INT(9) NOT NULL COMMENT '病历ID，指向medical_record(id)',
  `source` CHAR(1) DEFAULT 'M' COMMENT '病历来源：A=AI草稿生成，M=人工录入',
  `ai_request_snapshot` TEXT COMMENT 'AI生成时的请求快照JSON',
  `ai_result_snapshot` TEXT COMMENT 'AI生成时的结果快照JSON',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_medical_record_id` (`medical_record_id`),
  INDEX `idx_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病历元数据表';


