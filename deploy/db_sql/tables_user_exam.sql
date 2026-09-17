-- ----------------------------
-- 用户竞赛关联表（竞赛报名、成绩与排名）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_user_exam` (
  `user_exam_id` bigint unsigned NOT NULL COMMENT '用户竞赛关系id (主键，雪花算法)',
  `user_id` bigint unsigned NOT NULL COMMENT '用户id',
  `exam_id` bigint unsigned NOT NULL COMMENT '竞赛id',
  `score` int unsigned DEFAULT NULL COMMENT '得分',
  `exam_rank` int unsigned DEFAULT NULL COMMENT '排名',
  `create_by` bigint unsigned DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间/创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_exam_id`),
  UNIQUE KEY `uq_user_exam` (`user_id`, `exam_id`),
  KEY `idx_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户竞赛关联表';
