-- =============================================================
-- 增量脚本：题目用途（刷题 / 竞赛）（2026-09-26）
-- 适用：已用旧版 oj_init.sql 初始化过的库；新库直接执行 oj_init.sql 即可，不需要本脚本
-- 可重复执行：字段已存在时跳过加字段；只把「被尚未结束的竞赛使用的刷题题」标为竞赛题
-- 执行后重启 oj-friend，题目 ES 索引会带上用途重新同步
-- =============================================================

SET NAMES utf8mb4;
USE `bitoj_dev`;

-- 题目表加用途字段（MySQL 不支持 ADD COLUMN IF NOT EXISTS，先查再加）
SET @purpose_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'bitoj_dev' AND table_name = 'tb_question' AND column_name = 'purpose'
);
SET @ddl := IF(@purpose_exists = 0,
  'ALTER TABLE `tb_question` ADD COLUMN `purpose` tinyint NOT NULL DEFAULT ''1'' COMMENT ''题目用途 1: 刷题 2: 竞赛'' AFTER `difficulty`',
  'SELECT ''purpose 字段已存在，跳过'' AS info');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 被尚未结束的竞赛使用的题目标为竞赛题（竞赛题不出现在 C 端题库）
UPDATE `tb_question` q
SET q.`purpose` = 2
WHERE q.`purpose` = 1
  AND EXISTS (
    SELECT 1 FROM `tb_exam_question` eq
    INNER JOIN `tb_exam` e ON e.`exam_id` = eq.`exam_id`
    WHERE eq.`question_id` = q.`question_id` AND e.`end_time` > NOW()
  );

-- 核对：各用途的题目数
SELECT `purpose`, COUNT(*) AS question_count FROM `tb_question` GROUP BY `purpose`;
