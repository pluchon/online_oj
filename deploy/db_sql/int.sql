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


