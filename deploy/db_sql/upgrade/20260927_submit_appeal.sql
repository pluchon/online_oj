-- =============================================================
-- 增量脚本：提交申诉（2026-09-27）
-- 适用：已用旧版 oj_init.sql 初始化过的库；新库直接执行 oj_init.sql 即可，不需要本脚本
-- 可重复执行：字段已存在时跳过加字段，表已存在时跳过建表
-- =============================================================

SET NAMES utf8mb4;
USE `bitoj_dev`;

-- 提交表加逐用例结果字段（MySQL 不支持 ADD COLUMN IF NOT EXISTS，先查再加）
SET @case_outputs_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'bitoj_dev' AND table_name = 'tb_user_submit' AND column_name = 'case_outputs'
);
SET @ddl := IF(@case_outputs_exists = 0,
  'ALTER TABLE `tb_user_submit` ADD COLUMN `case_outputs` text DEFAULT NULL COMMENT ''逐用例结果(JSON：[{caseId, pass, output}]，只保存未通过用例的实际输出，每条截断)'' AFTER `case_states`',
  'SELECT ''case_outputs 字段已存在，跳过'' AS info');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 提交申诉表（学员认为判错时提出，AI 初审放行后才能创建；每条提交只能申诉一次）
CREATE TABLE IF NOT EXISTS `tb_submit_appeal` (
  `appeal_id` bigint unsigned NOT NULL COMMENT '申诉id(主键)',
  `submit_id` bigint unsigned NOT NULL COMMENT '被申诉的提交id',
  `user_id` bigint unsigned NOT NULL COMMENT '申诉人',
  `question_id` bigint unsigned NOT NULL COMMENT '题目id',
  `exam_id` bigint unsigned DEFAULT NULL COMMENT '竞赛id(为空表示练习提交)',
  `reason` varchar(500) NOT NULL COMMENT '申诉理由',
  `ai_analysis` varchar(2000) NOT NULL DEFAULT '' COMMENT 'AI 初审分析(只给管理员看)',
  `origin_judge_status` tinyint DEFAULT NULL COMMENT '申诉时的判题结论',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '申诉状态 0: 待处理 1: 存疑 2: 通过 3: 不通过',
  `handle_by` bigint unsigned DEFAULT NULL COMMENT '裁定人(管理员id)',
  `handle_time` datetime DEFAULT NULL COMMENT '裁定时间',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间(申诉时间)',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `delete_state` tinyint NOT NULL DEFAULT '0' COMMENT '0: 正常 1: 已删除',
  `active_submit_id` bigint unsigned GENERATED ALWAYS AS (IF(`delete_state` = 0, `submit_id`, NULL)) VIRTUAL COMMENT '未删除申诉的提交id(一条提交只能申诉一次)',
  PRIMARY KEY (`appeal_id`),
  UNIQUE KEY `uk_active_submit` (`active_submit_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user` (`user_id`),
  KEY `idx_question_status` (`question_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提交申诉表';
