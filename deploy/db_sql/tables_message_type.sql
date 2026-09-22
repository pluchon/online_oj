-- ----------------------------
-- 消息类型增量脚本（可重复执行）
-- 执行顺序：tables_message.sql 之后
-- 用途：消息中心按类型筛选；历史消息中的竞赛结果通知回填为竞赛通知，其余保持系统通知
-- ----------------------------

SET NAMES utf8mb4;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_message_text' AND COLUMN_NAME = 'message_type');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `tb_message_text`
     ADD COLUMN `message_type` tinyint NOT NULL DEFAULT ''1'' COMMENT ''消息类型 1: 系统通知 2: 竞赛通知'' AFTER `text_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `tb_message_text`
SET `message_type` = 2
WHERE `message_type` = 1
  AND (`message_title` = '竞赛结果通知' OR `message_title` LIKE '【竞赛%');
