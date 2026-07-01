-- ============================================================
-- 东软智慧云脑诊疗平台 - 增量脚本 v1.2
-- 处方审核记录表 (prescription_audit_record)
-- ============================================================
-- 用途：承接 AI 处方辅助审核的输入快照与结果留痕。
-- 触发：①医生点"AI审核"仅预览不落库；②确认/保存处方时审核并落本表（见 AI 接入决策3）。
-- 关联：register_id 指向 register(ID)，auditor_employee_id 指向 employee(ID)。
-- 本脚本由 docker-entrypoint-initdb.d 在首次建库时自动执行（排在 01/02 之后）。
-- 版本化参考副本见 sql/v1.2_prescription_audit_record.sql。
-- ============================================================

USE `hospital_cloud_brain`;

SET NAMES utf8mb4;

-- (17) 处方审核记录表 (prescription_audit_record)
-- 功能说明：记录每次 AI 处方审核的请求快照(药品+患者)、AI原始返回、总体风险等级及审核操作员，便于复盘与追溯。
CREATE TABLE IF NOT EXISTS `prescription_audit_record` (
  `id` INT(9) NOT NULL AUTO_INCREMENT COMMENT '自增长类型',
  `register_id` INT(9) DEFAULT NULL COMMENT '挂号ID，指向register(ID)',
  `request_snapshot` TEXT COMMENT '审核请求快照JSON(患者信息+药品明细)',
  `result_json` TEXT COMMENT 'AI完整返回JSON(含suggestions/interactions/riskItems)',
  `risk_level` VARCHAR(16) DEFAULT NULL COMMENT '总体风险等级：low/medium/high',
  `auditor_employee_id` INT(9) DEFAULT NULL COMMENT '审核操作员员工ID，指向employee(ID)',
  `creation_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_register_id` (`register_id`),
  INDEX `idx_auditor_employee_id` (`auditor_employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方审核记录表';
