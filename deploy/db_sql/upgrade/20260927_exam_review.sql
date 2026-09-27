-- =============================================================
-- 增量脚本：赛后复盘（2026-09-27）
-- 适用：已用旧版 oj_init.sql 初始化过的库；新库直接执行 oj_init.sql 即可，不需要本脚本
-- 可重复执行：表已存在时跳过建表
-- =============================================================

SET NAMES utf8mb4;
USE `bitoj_dev`;

-- 赛后复盘表（学员第一次打开时生成；这场的提交结果变化后再打开时原地覆盖，一人一场一条）
CREATE TABLE IF NOT EXISTS `tb_exam_review` (
  `review_id` bigint unsigned NOT NULL COMMENT '复盘id(主键)',
  `user_id` bigint unsigned NOT NULL COMMENT '学员id',
  `exam_id` bigint unsigned NOT NULL COMMENT '竞赛id',
  `content` text NOT NULL COMMENT '复盘内容(JSON：成绩概览、逐题统计与 AI 点评、整体总结)',
  `source_digest` char(32) NOT NULL COMMENT '生成时本人这场提交结果的摘要(MD5)，不一致时重新生成',
  `regenerate_count` int NOT NULL DEFAULT '0' COMMENT '学员手动重新生成的次数(每场最多 3 次，提交结果变化引起的重新生成不计)',
  `create_by` bigint unsigned DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `delete_state` tinyint NOT NULL DEFAULT '0' COMMENT '0: 正常 1: 已删除',
  PRIMARY KEY (`review_id`),
  UNIQUE KEY `uk_user_exam` (`user_id`, `exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='赛后复盘表';
