-- ----------------------------
-- 竞赛结算标记增量脚本（可重复执行）
-- 执行顺序：int.sql 之后
-- 用途：定时任务只结算未结算的已结束竞赛，保证排名落库与战报通知只执行一次
-- ----------------------------

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_exam' AND COLUMN_NAME = 'rank_settled');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `tb_exam`
     ADD COLUMN `rank_settled` tinyint NOT NULL DEFAULT ''0'' COMMENT ''排名是否已结算 0: 未结算 1: 已结算'' AFTER `status`,
     ADD KEY `idx_settle` (`rank_settled`, `end_time`)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
