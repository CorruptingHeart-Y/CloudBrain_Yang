-- ============================================================
-- v1.4 挂号接口加固 - register.case_number 唯一索引
-- ============================================================
-- 背景：挂号创建接口改为服务端生成 case_number，需以唯一索引兜底
--       并发冲突（BL+yyyyMMddHHmmss+2位随机 重复时由重试吸收）。
--
-- 说明：
--   * case_number 允许 NULL；InnoDB 唯一索引允许多个 NULL，不影响历史无号数据。
--   * 现有数据 case_number 均唯一，加索引不会失败。
--   * 若远程库存在重复 case_number，需先去重再加索引：
--       SELECT case_number, COUNT(*) c FROM register
--        WHERE case_number IS NOT NULL GROUP BY case_number HAVING c>1;
-- ============================================================

USE `hospital_cloud_brain`;
SET NAMES utf8mb4;

ALTER TABLE `register` ADD UNIQUE INDEX `uk_case_number` (`case_number`);

-- 校验
SELECT 'uk_case_number_added' AS metric,
       index_name, non_unique, column_name
FROM information_schema.STATISTICS
WHERE table_schema = DATABASE()
  AND table_name = 'register'
  AND index_name = 'uk_case_number';
