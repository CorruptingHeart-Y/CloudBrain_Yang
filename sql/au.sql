-- (5) 医院员工表 (employee)
-- 功能说明：保存医生等员工账户基础信息，建立科室与排班逻辑索引。
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
