create database if not exists bitoj_dev;

use bitoj_dev;

# 用户表
DROP TABLE IF EXISTS `tb_sys_user`;
CREATE TABLE `tb_sys_user` (
                               `user_id` bigint(20) unsigned NOT NULL COMMENT '⽤⼾id', # 表示不允许存在负数，并且不用自增主键
                               `user_account` varchar(32) DEFAULT NULL COMMENT '⽤⼾账号',# 用户的账号名
                               `password` varchar(100) DEFAULT NULL COMMENT '⽤⼾密码',
                               `nick_name` varchar(32) DEFAULT NULL COMMENT '昵称',
                               `create_by` bigint(8) NOT NULL COMMENT '创建⽤⼾',# 是谁添加的，便于追踪数据
                               `create_time` datetime NOT NULL COMMENT '创建时间',
                               `update_by` bigint(8) DEFAULT NULL COMMENT '更新⽤⼾',# 是谁更新的，便于追踪数据
                               `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                               PRIMARY KEY (`user_id`),
                               UNIQUE KEY `user_account` (`user_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端⽤⼾表';

# 初始管理员账号（账号: admin, 密码: 123456）
INSERT INTO `tb_sys_user` (`user_id`, `user_account`, `password`, `nick_name`, `create_by`, `create_time`)
VALUES (1, 'admin', '$2a$10$dBUEv31nO3S3L60wptbA4eGCCX8roQSJN.RDx5Z4mVMEi285Jgnq6', '超级管理员', 1, NOW())
ON DUPLICATE KEY UPDATE `password` = VALUES(`password`), `nick_name` = VALUES(`nick_name`);

# 题目表
DROP TABLE IF EXISTS `tb_question`;
CREATE TABLE `tb_question` (
    `question_id` bigint unsigned NOT NULL COMMENT '题目id',
    `title` varchar(50) NOT NULL COMMENT '题目标题',
    `difficulty` tinyint NOT NULL COMMENT '题目难度1:简单 2：中等 3：困难',
    `time_limit` int NOT NULL COMMENT '时间限制',
    `space_limit` int NOT NULL COMMENT '空间限制',
    `content` varchar(1000) NOT NULL COMMENT '题目内容',
    `question_case` varchar(1000) DEFAULT NULL COMMENT '题目用例',
    `default_code` varchar(500) NOT NULL COMMENT '默认代码块',
    `main_func` varchar(500) NOT NULL COMMENT 'main函数',
    `create_by` bigint unsigned NOT NULL COMMENT '创建人',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

# 初始题目测试数据（精选3道完整题目：两数之和、有效的括号、回文数，关联管理员 create_by = 1）
INSERT INTO `tb_question` (`question_id`, `title`, `difficulty`, `time_limit`, `space_limit`, `content`, `question_case`, `default_code`, `main_func`, `create_by`, `create_time`) VALUES
(1794933791345602562, '两数之和', 1, 1000, 128, '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值 target 的那两个整数，并返回它们的数组下标。你可以假设每种输入只会对应一个答案。但是，数组中同一个元素在答案里不能重复出现。', '[\n  {\"input\": \"nums = [2,7,11,15], target = 9\", \"output\": \"[0,1]\"},\n  {\"input\": \"nums = [3,2,4], target = 6\", \"output\": \"[1,2]\"},\n  {\"input\": \"nums = [3,3], target = 6\", \"output\": \"[0,1]\"}\n]', 'public int[] twoSum(int[] nums, int target) {\n    // 请在此处编写你的代码\n    return new int[0];\n}', 'public static void main(String[] args) {\n    Main m = new Main();\n    int[] a = m.twoSum(new int[]{2, 7, 11, 15}, 9);\n    if (a == null || a[0] + a[1] != 1 || a[0] * a[1] != 0) throw new RuntimeException(\"Case 1 Failed\");\n    int[] b = m.twoSum(new int[]{3, 2, 4}, 6);\n    if (b == null || b[0] + b[1] != 3 || b[0] * b[1] != 2) throw new RuntimeException(\"Case 2 Failed\");\n    System.out.println(\"OK\");\n}', 1, '2024-05-27 11:28:59'),
(1794900876543210003, '有效的括号', 1, 1000, 128, '给定一个只包括 \'(\', \')\', \'{\', \'}\', \'[\', \']\' 的字符串 s ，判断字符串是否有效。有效字符串需满足：左括号必须用相同类型的右括号闭合；左括号必须以正确的顺序闭合；每个右括号都有一个对应的相同类型的左括号。', '[\n  {\"input\": \"s = \\\"()\\\"\", \"output\": \"true\"},\n  {\"input\": \"s = \\\"()[]{}\\\"\", \"output\": \"true\"},\n  {\"input\": \"s = \\\"(]\\\"\", \"output\": \"false\"},\n  {\"input\": \"s = \\\"{[]}\\\"\", \"output\": \"true\"}\n]', 'public boolean isValid(String s) {\n    // 请在此处编写你的代码\n    return false;\n}', 'public static void main(String[] args) {\n    Main m = new Main();\n    if (!m.isValid(\"()\") || !m.isValid(\"()[]{}\")) throw new RuntimeException(\"Case 1 Failed\");\n    if (m.isValid(\"(]\") || m.isValid(\"([)]\")) throw new RuntimeException(\"Case 2 Failed\");\n    if (!m.isValid(\"{[]}\")) throw new RuntimeException(\"Case 3 Failed\");\n    System.out.println(\"OK\");\n}', 1, '2024-05-24 14:08:45'),
(1796119683661783042, '回文数', 1, 1000, 128, '给你一个整数 x ，如果 x 是一个回文整数，返回 true ；否则，返回 false 。回文数是指正序（从左向右）和倒序（从右向左）读都是一样的整数。例如，121 是回文，而 123 不是。负数如 -121 不是回文数。', '[\n  {\"input\": \"x = 121\", \"output\": \"true\"},\n  {\"input\": \"x = -121\", \"output\": \"false\"},\n  {\"input\": \"x = 10\", \"output\": \"false\"},\n  {\"input\": \"x = 0\", \"output\": \"true\"}\n]', 'public boolean isPalindrome(int x) {\n    // 请在此处编写你的代码\n    return false;\n}', 'public static void main(String[] args) {\n    Main m = new Main();\n    if (!m.isPalindrome(121)) throw new RuntimeException(\"Case 1 Failed\");\n    if (m.isPalindrome(-121) || m.isPalindrome(10)) throw new RuntimeException(\"Case 2 Failed\");\n    if (!m.isPalindrome(0)) throw new RuntimeException(\"Case 3 Failed\");\n    System.out.println(\"OK\");\n}', 1, '2024-05-30 18:01:18')
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `difficulty` = VALUES(`difficulty`), `create_by` = VALUES(`create_by`), `question_case` = VALUES(`question_case`), `default_code` = VALUES(`default_code`), `main_func` = VALUES(`main_func`);

# 竞赛表
DROP TABLE IF EXISTS `tb_exam`;
CREATE TABLE `tb_exam` (
    `exam_id` bigint unsigned NOT NULL COMMENT '竞赛id (主键)',
    `title` varchar(50) NOT NULL COMMENT '竞赛标题',
    `start_time` datetime NOT NULL COMMENT '竞赛开始时间',
    `end_time` datetime NOT NULL COMMENT '竞赛结束时间',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '是否发布 0: 未发布 1: 已发布',
    `create_by` bigint unsigned NOT NULL COMMENT '创建人',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛表';

# 竞赛题目关系表
DROP TABLE IF EXISTS `tb_exam_question`;
CREATE TABLE `tb_exam_question` (
    `exam_question_id` bigint unsigned NOT NULL COMMENT '竞赛题目关系id (主键)',
    `question_id` bigint unsigned NOT NULL COMMENT '题目id',
    `exam_id` bigint unsigned NOT NULL COMMENT '竞赛id',
    `question_order` int NOT NULL COMMENT '题目顺序',
    `create_by` bigint unsigned NOT NULL COMMENT '创建人',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`exam_question_id`),
    KEY `idx_exam_id` (`exam_id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛题目关系表';

# 初始竞赛测试数据（关联管理员 create_by = 1）
INSERT INTO `tb_exam` (`exam_id`, `title`, `start_time`, `end_time`, `status`, `create_by`, `create_time`) VALUES
(1800000000000000001, 'AAA', '2024-06-27 00:00:00', '2024-07-06 00:00:00', 1, 1, '2024-06-06 20:57:28'),
(1800000000000000002, '未开始竞赛002', '2034-05-30 17:41:32', '2034-06-30 23:00:00', 1, 1, '2024-05-30 17:41:57'),
(1800000000000000003, '未开始的竞赛', '2044-07-08 15:00:00', '2044-07-08 23:00:00', 1, 1, '2024-05-30 17:40:18'),
(1800000000000000004, '竞赛001', '2034-06-08 17:36:11', '2034-07-08 17:36:11', 1, 1, '2024-05-30 17:36:52'),
(1800000000000000005, '竞赛报名测试', '2024-05-30 17:36:00', '2025-07-31 00:00:00', 1, 1, '2024-05-30 17:35:08'),
(1800000000000000006, '竞赛答题测试', '2024-05-30 16:57:48', '2024-05-30 17:00:00', 1, 1, '2024-05-30 16:55:41'),
(1800000000000000007, '竞赛测试', '2024-05-29 00:00:00', '2024-06-30 00:00:00', 1, 1, '2024-05-27 11:29:45'),
(1800000000000000008, '草稿竞赛', '2034-08-01 09:00:00', '2034-08-01 12:00:00', 0, 1, '2024-05-30 18:00:00')
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `start_time` = VALUES(`start_time`), `end_time` = VALUES(`end_time`), `status` = VALUES(`status`);

# 普通用户表（C端用户）
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
    `user_id` bigint unsigned NOT NULL COMMENT '用户id (主键)',
    `nick_name` varchar(32) DEFAULT NULL COMMENT '用户昵称',
    `head_image` varchar(255) DEFAULT NULL COMMENT '用户头像',
    `sex` tinyint DEFAULT 0 COMMENT '用户性别 0: 保密 1: 男 2: 女',
    `phone` char(11) NOT NULL COMMENT '手机号',
    `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
    `wechat` varchar(32) DEFAULT NULL COMMENT '微信号',
    `qq` varchar(20) DEFAULT NULL COMMENT 'QQ号',
    `school_name` varchar(50) DEFAULT NULL COMMENT '学校',
    `major_name` varchar(50) DEFAULT NULL COMMENT '专业',
    `introduce` varchar(255) DEFAULT NULL COMMENT '个人介绍',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '用户状态 0: 拉黑 1: 正常',
    `create_by` bigint unsigned DEFAULT NULL COMMENT '创建人',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uq_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='普通用户表';

# 初始普通用户测试数据
INSERT INTO `tb_user` (`user_id`, `nick_name`, `head_image`, `sex`, `phone`, `email`, `school_name`, `major_name`, `introduce`, `status`, `create_time`) VALUES
(1700000000000000001, '编程小白', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 1, '13800000001', 'coder1@oj.com', '清华大学', '计算机科学与技术', 'Talk is cheap. Show me the code.', 1, NOW()),
(1700000000000000002, '算法达人', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 2, '13800000002', 'coder2@oj.com', '北京大学', '软件工程', '保持热爱，奔赴山海。', 1, NOW())
ON DUPLICATE KEY UPDATE `nick_name` = VALUES(`nick_name`), `head_image` = VALUES(`head_image`), `status` = VALUES(`status`);

# 用户代码提交表（判题模块核心表）
DROP TABLE IF EXISTS `tb_user_submit`;
CREATE TABLE `tb_user_submit` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户代码提交记录表';


