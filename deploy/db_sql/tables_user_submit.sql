-- ----------------------------
-- 用户代码提交记录表（判题模块核心表）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_user_submit` (
  `submit_id` bigint unsigned NOT NULL COMMENT '提交记录id (主键，雪花算法)',
  `user_id` bigint unsigned NOT NULL COMMENT '用户id',
  `question_id` bigint unsigned NOT NULL COMMENT '题目id',
  `exam_id` bigint unsigned DEFAULT NULL COMMENT '竞赛id (为空表示非竞赛练习提交)',
  `program_type` tinyint NOT NULL COMMENT '代码类型 0: java 1: CPP',
  `user_code` text NOT NULL COMMENT '用户代码',
  `pass` tinyint NOT NULL DEFAULT '0' COMMENT '判题结果 0: 未通过 1: 通过',
  `exe_message` varchar(2000) DEFAULT '' COMMENT '执行结果/报错信息',
  `score` int NOT NULL DEFAULT '0' COMMENT '得分',
  `create_by` bigint unsigned DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`submit_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户代码提交记录表';
