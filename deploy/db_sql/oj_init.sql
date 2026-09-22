-- =============================================================
-- 墨衡 OJ 数据库初始化脚本（业务库 bitoj_dev + 调度库 xxl_job）
-- 用法：docker compose 首次创建 MySQL 数据卷时自动执行；也可手动执行，重复执行不会覆盖已有数据
-- 内容：13 张业务表、测试数据（15 道题与用例、5 场竞赛、8 个用户、提交记录与站内消息）、XXL-JOB 表与任务
-- 测试账号：管理端 admin / 123456；用户端手机号 13800000001 ~ 13800000007（模拟发码模式下验证码输出在 oj-friend 控制台）
-- 竞赛时间以执行时刻为基准：1 场已结算、1 场已结束待结算、1 场进行中、1 场未开始、1 场未发布
-- 主键为雪花 ID（测试数据用固定值），题目用例表 case_id 为自增
-- =============================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `bitoj_dev` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `bitoj_dev`;

-- -------------------------------------------------------------
-- 一、表结构
-- -------------------------------------------------------------

-- 管理端用户表
CREATE TABLE IF NOT EXISTS `tb_sys_user` (
  `user_id` bigint unsigned NOT NULL COMMENT '用户id(主键)',
  `user_account` varchar(32) DEFAULT NULL COMMENT '用户账号',
  `password` varchar(100) DEFAULT NULL COMMENT '用户密码(BCrypt)',
  `nick_name` varchar(32) DEFAULT NULL COMMENT '昵称',
  `create_by` bigint NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_account` (`user_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理端用户表';

-- 用户端用户表
CREATE TABLE IF NOT EXISTS `tb_user` (
  `user_id` bigint unsigned NOT NULL COMMENT '用户id(主键)',
  `nick_name` varchar(32) DEFAULT NULL COMMENT '用户昵称',
  `head_image` varchar(255) DEFAULT NULL COMMENT '用户头像',
  `sex` tinyint DEFAULT '0' COMMENT '用户性别 0: 保密 1: 男 2: 女',
  `phone` char(11) NOT NULL COMMENT '手机号',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `wechat` varchar(32) DEFAULT NULL COMMENT '微信号',
  `qq` varchar(20) DEFAULT NULL COMMENT 'QQ号',
  `school_name` varchar(50) DEFAULT NULL COMMENT '学校',
  `major_name` varchar(50) DEFAULT NULL COMMENT '专业',
  `introduce` varchar(255) DEFAULT NULL COMMENT '个人介绍',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '用户状态 0: 拉黑 1: 正常',
  `create_by` bigint unsigned DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户端用户表';

-- 题目表
CREATE TABLE IF NOT EXISTS `tb_question` (
  `question_id` bigint unsigned NOT NULL COMMENT '题目id(主键)',
  `title` varchar(50) NOT NULL COMMENT '题目标题',
  `difficulty` tinyint NOT NULL COMMENT '题目难度 1: 简单 2: 中等 3: 困难',
  `time_limit` int NOT NULL COMMENT '时间限制(ms)',
  `space_limit` int NOT NULL COMMENT '空间限制(MB)',
  `content` varchar(1000) NOT NULL COMMENT '题目描述(Markdown)',
  `default_code` varchar(500) NOT NULL COMMENT '默认代码模板',
  `main_func` text NOT NULL COMMENT 'main函数（首行读用例数，逐用例读参数并每行输出一个结果）',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目表';

-- 题目测试用例表（公开示例与隐藏用例共表，is_sample 区分）
CREATE TABLE IF NOT EXISTS `tb_question_case` (
  `case_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用例id(主键)',
  `question_id` bigint unsigned NOT NULL COMMENT '题目id',
  `display_input` varchar(2000) NOT NULL COMMENT '展示用输入，如 s = "()"',
  `display_output` varchar(2000) NOT NULL COMMENT '展示用输出',
  `judge_input` text NOT NULL COMMENT '判题用输入，按 main 函数约定逐行给出参数',
  `judge_output` varchar(2000) NOT NULL COMMENT '判题用预期输出（单行）',
  `is_sample` tinyint NOT NULL DEFAULT '0' COMMENT '1: 公开示例 0: 隐藏用例',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序，升序',
  `create_by` bigint unsigned DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_state` tinyint NOT NULL DEFAULT '0' COMMENT '0: 正常 1: 已删除',
  PRIMARY KEY (`case_id`),
  KEY `idx_question_sample` (`question_id`,`is_sample`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目测试用例表';

-- 竞赛表
CREATE TABLE IF NOT EXISTS `tb_exam` (
  `exam_id` bigint unsigned NOT NULL COMMENT '竞赛id(主键)',
  `title` varchar(50) NOT NULL COMMENT '竞赛标题',
  `start_time` datetime NOT NULL COMMENT '竞赛开始时间',
  `end_time` datetime NOT NULL COMMENT '竞赛结束时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '是否发布 0: 未发布 1: 已发布',
  `rank_settled` tinyint NOT NULL DEFAULT '0' COMMENT '排名是否已结算 0: 未结算 1: 已结算',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`exam_id`),
  KEY `idx_settle` (`rank_settled`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='竞赛表';

-- 竞赛题目关系表
CREATE TABLE IF NOT EXISTS `tb_exam_question` (
  `exam_question_id` bigint unsigned NOT NULL COMMENT '竞赛题目关系id(主键)',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='竞赛题目关系表';

-- 用户竞赛关联表（报名、成绩与排名）
CREATE TABLE IF NOT EXISTS `tb_user_exam` (
  `user_exam_id` bigint unsigned NOT NULL COMMENT '用户竞赛关系id(主键)',
  `user_id` bigint unsigned NOT NULL COMMENT '用户id',
  `exam_id` bigint unsigned NOT NULL COMMENT '竞赛id',
  `score` int unsigned DEFAULT NULL COMMENT '得分(结算后写入)',
  `exam_rank` int unsigned DEFAULT NULL COMMENT '排名(结算后写入)',
  `create_by` bigint unsigned DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_exam_id`),
  UNIQUE KEY `uq_user_exam` (`user_id`,`exam_id`),
  KEY `idx_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户竞赛关联表';

-- 用户代码提交记录表
CREATE TABLE IF NOT EXISTS `tb_user_submit` (
  `submit_id` bigint unsigned NOT NULL COMMENT '提交记录id(主键)',
  `user_id` bigint unsigned NOT NULL COMMENT '用户id',
  `question_id` bigint unsigned NOT NULL COMMENT '题目id',
  `exam_id` bigint unsigned DEFAULT NULL COMMENT '竞赛id(为空表示练习提交)',
  `program_type` tinyint NOT NULL COMMENT '代码类型 0: Java',
  `user_code` text NOT NULL COMMENT '用户代码',
  `pass` tinyint NOT NULL DEFAULT '0' COMMENT '判题结果 0: 未通过 1: 通过 2: 评测中',
  `exe_message` varchar(2000) DEFAULT '' COMMENT '执行结果/报错信息',
  `score` int NOT NULL DEFAULT '0' COMMENT '得分',
  `judge_status` tinyint DEFAULT NULL COMMENT '判题状态 1:AC 2:WA 3:TLE 4:MLE 5:CE 6:RE 8:SE',
  `pass_count` int NOT NULL DEFAULT '0' COMMENT '通过用例数',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '总用例数',
  `time_cost` int DEFAULT NULL COMMENT '执行耗时(ms)',
  `fail_case_id` bigint unsigned DEFAULT NULL COMMENT '首个未通过用例id',
  `fail_output` varchar(2000) DEFAULT NULL COMMENT '首个未通过用例的实际输出',
  `case_states` varchar(500) DEFAULT NULL COMMENT '逐用例状态 1:通过 0:未通过 -:未执行',
  `create_by` bigint unsigned DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`submit_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户代码提交记录表';

-- 消息正文表
CREATE TABLE IF NOT EXISTS `tb_message_text` (
  `text_id` bigint unsigned NOT NULL COMMENT '消息内容id(主键)',
  `message_type` tinyint NOT NULL DEFAULT '1' COMMENT '消息类型 1: 系统通知 2: 竞赛通知',
  `message_title` varchar(50) NOT NULL COMMENT '消息标题',
  `message_content` varchar(500) NOT NULL COMMENT '消息内容',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`text_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息正文表';

-- 用户消息投递表
CREATE TABLE IF NOT EXISTS `tb_message` (
  `message_id` bigint unsigned NOT NULL COMMENT '消息id(主键)',
  `text_id` bigint unsigned NOT NULL COMMENT '关联的消息内容id',
  `send_id` bigint unsigned NOT NULL COMMENT '发送人id(0 代表系统)',
  `rec_id` bigint unsigned NOT NULL COMMENT '接收人id',
  `is_read` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否已读 0: 未读 1: 已读',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_rec_read` (`rec_id`,`is_read`,`create_time`),
  KEY `idx_text_id` (`text_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户消息投递表';

-- 做题代码草稿表（每个用户每道题一份）
CREATE TABLE IF NOT EXISTS `tb_user_code_draft` (
  `draft_id` bigint unsigned NOT NULL COMMENT '草稿id(主键)',
  `user_id` bigint unsigned NOT NULL COMMENT '用户id',
  `question_id` bigint unsigned NOT NULL COMMENT '题目id',
  `code` text NOT NULL COMMENT '代码内容',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`draft_id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='做题代码草稿表';

-- AI 做题辅导会话表（每个用户每道题一个）
CREATE TABLE IF NOT EXISTS `tb_ai_chat_session` (
  `session_id` bigint unsigned NOT NULL COMMENT '会话id(主键)',
  `user_id` bigint unsigned NOT NULL COMMENT '用户id',
  `question_id` bigint unsigned NOT NULL COMMENT '题目id',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 做题辅导会话表';

-- AI 做题辅导消息表（同时用于每日次数统计与审计）
CREATE TABLE IF NOT EXISTS `tb_ai_chat_message` (
  `message_id` bigint unsigned NOT NULL COMMENT '消息id(主键)',
  `session_id` bigint unsigned NOT NULL COMMENT '会话id',
  `user_id` bigint unsigned NOT NULL COMMENT '用户id',
  `role` tinyint unsigned NOT NULL COMMENT '角色 1: 用户 2: AI',
  `action` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '提问类型 0: 自由提问 1: 指点迷津 2: 分析提交 3: 解释编译错误 4: 代码点评 5: 优化代码思路',
  `content` text NOT NULL COMMENT '消息内容',
  `model` varchar(64) DEFAULT NULL COMMENT '生成回复的模型(仅 AI 消息)',
  `prompt_tokens` int unsigned DEFAULT NULL COMMENT '输入 Token 数(仅 AI 消息)',
  `completion_tokens` int unsigned DEFAULT NULL COMMENT '输出 Token 数(仅 AI 消息)',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_session_time` (`session_id`,`create_time`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 做题辅导消息表';

-- -------------------------------------------------------------
-- 二、测试数据
-- -------------------------------------------------------------

-- 管理员（admin / 123456）
INSERT IGNORE INTO `tb_sys_user` (`user_id`, `user_account`, `password`, `nick_name`, `create_by`, `create_time`) VALUES
(1, 'admin', '$2a$10$dBUEv31nO3S3L60wptbA4eGCCX8roQSJN.RDx5Z4mVMEi285Jgnq6', '超级管理员', 1, DATE_SUB(NOW(), INTERVAL 129600 MINUTE));

-- 用户端用户（最后一个为拉黑状态）
INSERT IGNORE INTO `tb_user` (`user_id`, `nick_name`, `head_image`, `sex`, `phone`, `email`, `wechat`, `qq`, `school_name`, `major_name`, `introduce`, `status`, `create_time`) VALUES
(1700000000000000001, '编程小白', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 1, '13800000001', 'coder1@example.com', NULL, '10001', '清华大学', '计算机科学与技术', 'Talk is cheap. Show me the code.', 1, DATE_SUB(NOW(), INTERVAL 86400 MINUTE)),
(1700000000000000002, '算法达人', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 2, '13800000002', 'coder2@example.com', 'algo_master', NULL, '北京大学', '软件工程', '保持热爱，奔赴山海。', 1, DATE_SUB(NOW(), INTERVAL 79200 MINUTE)),
(1700000000000000003, '夜航船', NULL, 0, '13800000003', 'night.boat@example.com', NULL, NULL, '浙江大学', '数学与应用数学', '每天一道题。', 1, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1700000000000000004, '青柠', NULL, 2, '13800000004', NULL, NULL, NULL, '复旦大学', '信息安全', NULL, 1, DATE_SUB(NOW(), INTERVAL 64800 MINUTE)),
(1700000000000000005, '代码搬运工', NULL, 1, '13800000005', 'porter@example.com', NULL, '10005', '南京大学', '人工智能', '先跑通，再优化。', 1, DATE_SUB(NOW(), INTERVAL 57600 MINUTE)),
(1700000000000000006, '墨白', NULL, 0, '13800000006', NULL, NULL, NULL, NULL, NULL, NULL, 1, DATE_SUB(NOW(), INTERVAL 50400 MINUTE)),
(1700000000000000007, '小鹿乱撞', NULL, 2, '13800000007', 'deer@example.com', NULL, NULL, '武汉大学', '数据科学与大数据技术', '刚开始学算法。', 1, DATE_SUB(NOW(), INTERVAL 43200 MINUTE)),
(1700000000000000008, '违规账号', NULL, 0, '13800000008', NULL, NULL, NULL, NULL, NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 36000 MINUTE));

-- 题目（简单 6 道、中等 5 道、困难 4 道）
INSERT IGNORE INTO `tb_question` (`question_id`, `title`, `difficulty`, `time_limit`, `space_limit`, `content`, `default_code`, `main_func`, `create_by`, `create_time`) VALUES
(1794933791345602562, '两数之和', 1, 1000, 128, '给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出和为目标值 `target` 的那两个整数，并返回它们的数组下标（按升序）。\n\n你可以假设每种输入只会对应一个答案，并且同一个元素在答案里不能重复出现。\n\n提示：\n- 2 <= nums.length <= 10^4\n- -10^9 <= nums[i] <= 10^9\n- 只会存在一个有效答案', 'public int[] twoSum(int[] nums, int target) {\n    // 请在此处编写你的代码\n    return new int[0];\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int[] nums = Arrays.stream(in.readLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();\n        int target = Integer.parseInt(in.readLine().trim());\n        int[] r = m.twoSum(nums, target);\n        if (r == null) {\n            System.out.println("null");\n        } else {\n            Arrays.sort(r);\n            System.out.println(Arrays.toString(r).replace(" ", ""));\n        }\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 115200 MINUTE)),
(1794900876543210003, '有效的括号', 1, 1000, 128, '给定一个只包括 `(`、`)`、`{`、`}`、`[`、`]` 的字符串 `s`，判断字符串是否有效。\n\n有效字符串需满足：\n1. 左括号必须用相同类型的右括号闭合；\n2. 左括号必须以正确的顺序闭合；\n3. 每个右括号都有一个对应的相同类型的左括号。\n\n提示：\n- 1 <= s.length <= 10^4\n- s 仅由括号 ()[]{} 组成', 'public boolean isValid(String s) {\n    // 请在此处编写你的代码\n    return false;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        String s = in.readLine();\n        System.out.println(m.isValid(s));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 110880 MINUTE)),
(1796119683661783042, '回文数', 1, 1000, 128, '给你一个整数 `x`，如果 `x` 是一个回文整数，返回 `true`；否则，返回 `false`。\n\n回文数是指正序（从左向右）和倒序（从右向左）读都是一样的整数，例如 121 是回文，而 123 不是。\n\n提示：\n- -2^31 <= x <= 2^31 - 1\n- 进阶：不将整数转为字符串来解决', 'public boolean isPalindrome(int x) {\n    // 请在此处编写你的代码\n    return false;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int x = Integer.parseInt(in.readLine().trim());\n        System.out.println(m.isPalindrome(x));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 106560 MINUTE)),
(1795000000000000104, '爬楼梯', 1, 1000, 128, '假设你正在爬楼梯，需要 `n` 阶才能到达楼顶。\n\n每次你可以爬 1 或 2 个台阶。你有多少种不同的方法可以爬到楼顶呢？\n\n提示：\n- 1 <= n <= 45', 'public int climbStairs(int n) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int n = Integer.parseInt(in.readLine().trim());\n        System.out.println(m.climbStairs(n));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 102240 MINUTE)),
(1795000000000000105, '只出现一次的数字', 1, 1000, 128, '给你一个非空整数数组 `nums`，除了某个元素只出现一次以外，其余每个元素均出现两次。找出那个只出现了一次的元素。\n\n你必须设计并实现线性时间复杂度的算法来解决此问题，且该算法只使用常量额外空间。\n\n提示：\n- 1 <= nums.length <= 3 * 10^4\n- -3 * 10^4 <= nums[i] <= 3 * 10^4\n- 除了某个元素只出现一次以外，其余每个元素均出现两次', 'public int singleNumber(int[] nums) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int[] nums = Arrays.stream(in.readLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();\n        System.out.println(m.singleNumber(nums));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 97920 MINUTE)),
(1795000000000000106, '二分查找', 1, 1000, 128, '给定一个 `n` 个元素有序的（升序）整型数组 `nums` 和一个目标值 `target`，写一个函数搜索 `nums` 中的 `target`，如果目标值存在返回下标，否则返回 `-1`。\n\n提示：\n- nums 中的所有元素互不相同\n- 1 <= n <= 10^4\n- -10^4 <= nums[i], target <= 10^4', 'public int search(int[] nums, int target) {\n    // 请在此处编写你的代码\n    return -1;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int[] nums = Arrays.stream(in.readLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();\n        int target = Integer.parseInt(in.readLine().trim());\n        System.out.println(m.search(nums, target));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 93600 MINUTE)),
(2102360449353351170, '岛屿数量统计', 2, 1000, 64, '给定一个由 `1`（陆地）和 `0`（水）组成的二维网格，请计算网格中岛屿的数量。\n\n岛屿总是被水包围，并且每座岛屿只能由水平方向和/或竖直方向上相邻的陆地连接形成。你可以假设网格的四条边均被水包围。\n\n提示：\n- 1 <= m, n <= 300\n- grid[i][j] 的值仅为 0 或 1', 'public int numIslands(char[][] grid) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int n = Integer.parseInt(in.readLine().trim());\n        char[][] grid = new char[n][];\n        for (int j = 0; j < n; j++) {\n            grid[j] = in.readLine().trim().toCharArray();\n        }\n        System.out.println(m.numIslands(grid));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 89280 MINUTE)),
(1795000000000000108, '无重复字符的最长子串', 2, 1000, 128, '给定一个字符串 `s`，请你找出其中不含有重复字符的最长子串的长度。\n\n提示：\n- 1 <= s.length <= 5 * 10^4\n- s 由英文字母、数字、符号和空格组成', 'public int lengthOfLongestSubstring(String s) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        String s = in.readLine();\n        System.out.println(m.lengthOfLongestSubstring(s == null ? "" : s));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 84960 MINUTE)),
(1795000000000000109, '最长递增子序列', 2, 1000, 128, '给你一个整数数组 `nums`，找到其中最长严格递增子序列的长度。\n\n子序列是由数组派生而来的序列，删除（或不删除）数组中的元素而不改变其余元素的顺序。\n\n提示：\n- 1 <= nums.length <= 2500\n- -10^4 <= nums[i] <= 10^4\n- 进阶：设计时间复杂度为 O(n log n) 的算法', 'public int lengthOfLIS(int[] nums) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int[] nums = Arrays.stream(in.readLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();\n        System.out.println(m.lengthOfLIS(nums));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 80640 MINUTE)),
(1795000000000000110, '零钱兑换', 2, 1000, 128, '给你一个整数数组 `coins`，表示不同面额的硬币；以及一个整数 `amount`，表示总金额。\n\n计算并返回可以凑成总金额所需的最少硬币个数。如果没有任何一种硬币组合能组成总金额，返回 `-1`。你可以认为每种硬币的数量是无限的。\n\n提示：\n- 1 <= coins.length <= 12\n- 1 <= coins[i] <= 2^31 - 1\n- 0 <= amount <= 10^4', 'public int coinChange(int[] coins, int amount) {\n    // 请在此处编写你的代码\n    return -1;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int[] coins = Arrays.stream(in.readLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();\n        int amount = Integer.parseInt(in.readLine().trim());\n        System.out.println(m.coinChange(coins, amount));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 76320 MINUTE)),
(1795000000000000111, '最大子数组和', 2, 1000, 128, '给你一个整数数组 `nums`，请你找出一个具有最大和的连续子数组（子数组最少包含一个元素），返回其最大和。\n\n提示：\n- 1 <= nums.length <= 10^5\n- -10^4 <= nums[i] <= 10^4\n- 进阶：尝试使用分治法求解', 'public int maxSubArray(int[] nums) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int[] nums = Arrays.stream(in.readLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();\n        System.out.println(m.maxSubArray(nums));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1795000000000000112, '接雨水', 3, 1000, 128, '给定 `n` 个非负整数表示每个宽度为 1 的柱子的高度图，计算按此排列的柱子，下雨之后能接多少雨水。\n\n提示：\n- 1 <= n <= 2 * 10^4\n- 0 <= height[i] <= 10^5', 'public int trap(int[] height) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int[] height = Arrays.stream(in.readLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();\n        System.out.println(m.trap(height));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 67680 MINUTE)),
(1795000000000000113, '编辑距离', 3, 1000, 128, '给你两个单词 `word1` 和 `word2`，请返回将 `word1` 转换成 `word2` 所使用的最少操作数。\n\n你可以对一个单词进行如下三种操作：插入一个字符、删除一个字符、替换一个字符。\n\n提示：\n- 1 <= word1.length, word2.length <= 500\n- word1 和 word2 由小写英文字母组成', 'public int minDistance(String word1, String word2) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        String a = in.readLine().trim();\n        String b = in.readLine().trim();\n        System.out.println(m.minDistance(a, b));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 63360 MINUTE)),
(1795000000000000114, '最长有效括号', 3, 1000, 128, '给你一个只包含 `(` 和 `)` 的字符串，找出最长有效（格式正确且连续）括号子串的长度。\n\n提示：\n- 1 <= s.length <= 3 * 10^4\n- s[i] 为 ( 或 )', 'public int longestValidParentheses(String s) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        String s = in.readLine().trim();\n        System.out.println(m.longestValidParentheses(s));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 59040 MINUTE)),
(1795000000000000115, 'N 皇后 II', 3, 2000, 128, 'n 皇后问题研究的是如何将 `n` 个皇后放置在 `n × n` 的棋盘上，并且使皇后彼此之间不能相互攻击（任意两个皇后不在同一行、同一列或同一斜线上）。\n\n给你一个整数 `n`，返回 n 皇后问题不同的解决方案的数量。\n\n提示：\n- 1 <= n <= 9', 'public int totalNQueens(int n) {\n    // 请在此处编写你的代码\n    return 0;\n}', 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int n = Integer.parseInt(in.readLine().trim());\n        System.out.println(m.totalNQueens(n));\n    }\n}', 1, DATE_SUB(NOW(), INTERVAL 54720 MINUTE));

-- 题目用例（该题尚无有效用例时才写入；预期输出由参考解实跑得到）
-- 两数之和
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1794933791345602562 AS qid, 'nums = [2,7,11,15], target = 9' AS di, '[0,1]' AS do_, '2,7,11,15\n9' AS ji, '[0,1]' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1794933791345602562, 'nums = [3,2,4], target = 6', '[1,2]', '3,2,4\n6', '[1,2]', 1, 2, 1
  UNION ALL SELECT 1794933791345602562, 'nums = [3,3], target = 6', '[0,1]', '3,3\n6', '[0,1]', 0, 3, 1
  UNION ALL SELECT 1794933791345602562, 'nums = [-1,-2,-3,-4,-5], target = -8', '[2,4]', '-1,-2,-3,-4,-5\n-8', '[2,4]', 0, 4, 1
  UNION ALL SELECT 1794933791345602562, 'nums = [0,4,3,0], target = 0', '[0,3]', '0,4,3,0\n0', '[0,3]', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1794933791345602562 AND `delete_state` = 0);
-- 有效的括号
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1794900876543210003 AS qid, 's = "()"' AS di, 'true' AS do_, '()' AS ji, 'true' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1794900876543210003, 's = "()[]{}"', 'true', '()[]{}', 'true', 1, 2, 1
  UNION ALL SELECT 1794900876543210003, 's = "(]"', 'false', '(]', 'false', 1, 3, 1
  UNION ALL SELECT 1794900876543210003, 's = "([)]"', 'false', '([)]', 'false', 0, 4, 1
  UNION ALL SELECT 1794900876543210003, 's = "{[]}"', 'true', '{[]}', 'true', 0, 5, 1
  UNION ALL SELECT 1794900876543210003, 's = "(("', 'false', '((', 'false', 0, 6, 1
  UNION ALL SELECT 1794900876543210003, 's = "){"', 'false', '){', 'false', 0, 7, 1
  UNION ALL SELECT 1794900876543210003, 's = "[({})]"', 'true', '[({})]', 'true', 0, 8, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1794900876543210003 AND `delete_state` = 0);
-- 回文数
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1796119683661783042 AS qid, 'x = 121' AS di, 'true' AS do_, '121' AS ji, 'true' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1796119683661783042, 'x = -121', 'false', '-121', 'false', 1, 2, 1
  UNION ALL SELECT 1796119683661783042, 'x = 10', 'false', '10', 'false', 1, 3, 1
  UNION ALL SELECT 1796119683661783042, 'x = 0', 'true', '0', 'true', 0, 4, 1
  UNION ALL SELECT 1796119683661783042, 'x = 12321', 'true', '12321', 'true', 0, 5, 1
  UNION ALL SELECT 1796119683661783042, 'x = 1000021', 'false', '1000021', 'false', 0, 6, 1
  UNION ALL SELECT 1796119683661783042, 'x = 2147447412', 'true', '2147447412', 'true', 0, 7, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1796119683661783042 AND `delete_state` = 0);
-- 爬楼梯
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000104 AS qid, 'n = 2' AS di, '2' AS do_, '2' AS ji, '2' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000104, 'n = 3', '3', '3', '3', 1, 2, 1
  UNION ALL SELECT 1795000000000000104, 'n = 1', '1', '1', '1', 0, 3, 1
  UNION ALL SELECT 1795000000000000104, 'n = 5', '8', '5', '8', 0, 4, 1
  UNION ALL SELECT 1795000000000000104, 'n = 10', '89', '10', '89', 0, 5, 1
  UNION ALL SELECT 1795000000000000104, 'n = 45', '1836311903', '45', '1836311903', 0, 6, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000104 AND `delete_state` = 0);
-- 只出现一次的数字
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000105 AS qid, 'nums = [2,2,1]' AS di, '1' AS do_, '2,2,1' AS ji, '1' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000105, 'nums = [4,1,2,1,2]', '4', '4,1,2,1,2', '4', 1, 2, 1
  UNION ALL SELECT 1795000000000000105, 'nums = [1]', '1', '1', '1', 0, 3, 1
  UNION ALL SELECT 1795000000000000105, 'nums = [-3,7,7]', '-3', '-3,7,7', '-3', 0, 4, 1
  UNION ALL SELECT 1795000000000000105, 'nums = [0,5,5,9,9]', '0', '0,5,5,9,9', '0', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000105 AND `delete_state` = 0);
-- 二分查找
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000106 AS qid, 'nums = [-1,0,3,5,9,12], target = 9' AS di, '4' AS do_, '-1,0,3,5,9,12\n9' AS ji, '4' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000106, 'nums = [-1,0,3,5,9,12], target = 2', '-1', '-1,0,3,5,9,12\n2', '-1', 1, 2, 1
  UNION ALL SELECT 1795000000000000106, 'nums = [5], target = 5', '0', '5\n5', '0', 0, 3, 1
  UNION ALL SELECT 1795000000000000106, 'nums = [1,3], target = 0', '-1', '1,3\n0', '-1', 0, 4, 1
  UNION ALL SELECT 1795000000000000106, 'nums = [2,4,6,8,10,12,14], target = 14', '6', '2,4,6,8,10,12,14\n14', '6', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000106 AND `delete_state` = 0);
-- 岛屿数量统计
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 2102360449353351170 AS qid, 'grid = [[''1'',''1'',''1'',''1'',''0''],[''1'',''1'',''0'',''1'',''0''],[''1'',''1'',''0'',''0'',''0''],[''0'',''0'',''0'',''0'',''0'']]' AS di, '1' AS do_, '4\n11110\n11010\n11000\n00000' AS ji, '1' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 2102360449353351170, 'grid = [[''1'',''1'',''0'',''0'',''0''],[''1'',''1'',''0'',''0'',''0''],[''0'',''0'',''1'',''0'',''0''],[''0'',''0'',''0'',''1'',''1'']]', '3', '4\n11000\n11000\n00100\n00011', '3', 1, 2, 1
  UNION ALL SELECT 2102360449353351170, 'grid = [[''0'']]', '0', '1\n0', '0', 0, 3, 1
  UNION ALL SELECT 2102360449353351170, 'grid = [[''1'']]', '1', '1\n1', '1', 0, 4, 1
  UNION ALL SELECT 2102360449353351170, 'grid = [[''1'',''0'',''1'',''0'',''1''],[''0'',''1'',''0'',''1'',''0''],[''1'',''0'',''1'',''0'',''1''],[''0'',''1'',''0'',''1'',''0''],[''1'',''0'',''1'',''0'',''1'']]', '13', '5\n10101\n01010\n10101\n01010\n10101', '13', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 2102360449353351170 AND `delete_state` = 0);
-- 无重复字符的最长子串
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000108 AS qid, 's = "abcabcbb"' AS di, '3' AS do_, 'abcabcbb' AS ji, '3' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000108, 's = "bbbbb"', '1', 'bbbbb', '1', 1, 2, 1
  UNION ALL SELECT 1795000000000000108, 's = "pwwkew"', '3', 'pwwkew', '3', 1, 3, 1
  UNION ALL SELECT 1795000000000000108, 's = "dvdf"', '3', 'dvdf', '3', 0, 4, 1
  UNION ALL SELECT 1795000000000000108, 's = "a"', '1', 'a', '1', 0, 5, 1
  UNION ALL SELECT 1795000000000000108, 's = "abba"', '2', 'abba', '2', 0, 6, 1
  UNION ALL SELECT 1795000000000000108, 's = "tmmzuxt"', '5', 'tmmzuxt', '5', 0, 7, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000108 AND `delete_state` = 0);
-- 最长递增子序列
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000109 AS qid, 'nums = [10,9,2,5,3,7,101,18]' AS di, '4' AS do_, '10,9,2,5,3,7,101,18' AS ji, '4' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000109, 'nums = [0,1,0,3,2,3]', '4', '0,1,0,3,2,3', '4', 1, 2, 1
  UNION ALL SELECT 1795000000000000109, 'nums = [7,7,7,7]', '1', '7,7,7,7', '1', 0, 3, 1
  UNION ALL SELECT 1795000000000000109, 'nums = [1]', '1', '1', '1', 0, 4, 1
  UNION ALL SELECT 1795000000000000109, 'nums = [4,10,4,3,8,9]', '3', '4,10,4,3,8,9', '3', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000109 AND `delete_state` = 0);
-- 零钱兑换
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000110 AS qid, 'coins = [1,2,5], amount = 11' AS di, '3' AS do_, '1,2,5\n11' AS ji, '3' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000110, 'coins = [2], amount = 3', '-1', '2\n3', '-1', 1, 2, 1
  UNION ALL SELECT 1795000000000000110, 'coins = [1], amount = 0', '0', '1\n0', '0', 0, 3, 1
  UNION ALL SELECT 1795000000000000110, 'coins = [186,419,83,408], amount = 6249', '20', '186,419,83,408\n6249', '20', 0, 4, 1
  UNION ALL SELECT 1795000000000000110, 'coins = [2,5,10,1], amount = 27', '4', '2,5,10,1\n27', '4', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000110 AND `delete_state` = 0);
-- 最大子数组和
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000111 AS qid, 'nums = [-2,1,-3,4,-1,2,1,-5,4]' AS di, '6' AS do_, '-2,1,-3,4,-1,2,1,-5,4' AS ji, '6' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000111, 'nums = [1]', '1', '1', '1', 1, 2, 1
  UNION ALL SELECT 1795000000000000111, 'nums = [5,4,-1,7,8]', '23', '5,4,-1,7,8', '23', 0, 3, 1
  UNION ALL SELECT 1795000000000000111, 'nums = [-3,-2,-5]', '-2', '-3,-2,-5', '-2', 0, 4, 1
  UNION ALL SELECT 1795000000000000111, 'nums = [2,-1,2,-1,2]', '4', '2,-1,2,-1,2', '4', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000111 AND `delete_state` = 0);
-- 接雨水
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000112 AS qid, 'height = [0,1,0,2,1,0,1,3,2,1,2,1]' AS di, '6' AS do_, '0,1,0,2,1,0,1,3,2,1,2,1' AS ji, '6' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000112, 'height = [4,2,0,3,2,5]', '9', '4,2,0,3,2,5', '9', 1, 2, 1
  UNION ALL SELECT 1795000000000000112, 'height = [1]', '0', '1', '0', 0, 3, 1
  UNION ALL SELECT 1795000000000000112, 'height = [5,4,1,2]', '1', '5,4,1,2', '1', 0, 4, 1
  UNION ALL SELECT 1795000000000000112, 'height = [2,0,2]', '2', '2,0,2', '2', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000112 AND `delete_state` = 0);
-- 编辑距离
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000113 AS qid, 'word1 = "horse", word2 = "ros"' AS di, '3' AS do_, 'horse\nros' AS ji, '3' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000113, 'word1 = "intention", word2 = "execution"', '5', 'intention\nexecution', '5', 1, 2, 1
  UNION ALL SELECT 1795000000000000113, 'word1 = "a", word2 = "b"', '1', 'a\nb', '1', 0, 3, 1
  UNION ALL SELECT 1795000000000000113, 'word1 = "abc", word2 = "abc"', '0', 'abc\nabc', '0', 0, 4, 1
  UNION ALL SELECT 1795000000000000113, 'word1 = "kitten", word2 = "sitting"', '3', 'kitten\nsitting', '3', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000113 AND `delete_state` = 0);
-- 最长有效括号
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000114 AS qid, 's = "(()"' AS di, '2' AS do_, '(()' AS ji, '2' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000114, 's = ")()())"', '4', ')()())', '4', 1, 2, 1
  UNION ALL SELECT 1795000000000000114, 's = "()(())"', '6', '()(())', '6', 0, 3, 1
  UNION ALL SELECT 1795000000000000114, 's = "((("', '0', '(((', '0', 0, 4, 1
  UNION ALL SELECT 1795000000000000114, 's = "()(()"', '2', '()(()', '2', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000114 AND `delete_state` = 0);
-- N 皇后 II
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1795000000000000115 AS qid, 'n = 4' AS di, '2' AS do_, '4' AS ji, '2' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1795000000000000115, 'n = 1', '1', '1', '1', 1, 2, 1
  UNION ALL SELECT 1795000000000000115, 'n = 5', '10', '5', '10', 0, 3, 1
  UNION ALL SELECT 1795000000000000115, 'n = 6', '4', '6', '4', 0, 4, 1
  UNION ALL SELECT 1795000000000000115, 'n = 8', '92', '8', '92', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1795000000000000115 AND `delete_state` = 0);

-- 竞赛（时间相对执行时刻：已结算 / 已结束待结算 / 进行中 / 未开始 / 未发布）
INSERT IGNORE INTO `tb_exam` (`exam_id`, `title`, `start_time`, `end_time`, `status`, `rank_settled`, `create_by`, `create_time`) VALUES
(1800000000000000001, '新手入门周赛 · 第 1 期', DATE_SUB(NOW(), INTERVAL 43200 MINUTE), DATE_SUB(NOW(), INTERVAL 43080 MINUTE), 1, 1, 1, DATE_SUB(NOW(), INTERVAL 47520 MINUTE)),
(1800000000000000002, '新手入门周赛 · 第 2 期', DATE_SUB(NOW(), INTERVAL 2880 MINUTE), DATE_SUB(NOW(), INTERVAL 2760 MINUTE), 1, 0, 1, DATE_SUB(NOW(), INTERVAL 7200 MINUTE)),
(1800000000000000003, '动态规划专题赛', DATE_SUB(NOW(), INTERVAL 1440 MINUTE), DATE_ADD(NOW(), INTERVAL 8640 MINUTE), 1, 0, 1, DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(1800000000000000004, '算法进阶挑战赛', DATE_ADD(NOW(), INTERVAL 4320 MINUTE), DATE_ADD(NOW(), INTERVAL 4500 MINUTE), 1, 0, 1, DATE_SUB(NOW(), INTERVAL 0 MINUTE)),
(1800000000000000005, '图论专题练习', DATE_ADD(NOW(), INTERVAL 14400 MINUTE), DATE_ADD(NOW(), INTERVAL 14520 MINUTE), 0, 0, 1, DATE_ADD(NOW(), INTERVAL 10080 MINUTE));

-- 竞赛题目
INSERT IGNORE INTO `tb_exam_question` (`exam_question_id`, `question_id`, `exam_id`, `question_order`, `create_by`, `create_time`) VALUES
(1830000000000000001, 1794933791345602562, 1800000000000000001, 1, 1, DATE_SUB(NOW(), INTERVAL 47520 MINUTE)),
(1830000000000000002, 1794900876543210003, 1800000000000000001, 2, 1, DATE_SUB(NOW(), INTERVAL 47520 MINUTE)),
(1830000000000000003, 1795000000000000104, 1800000000000000001, 3, 1, DATE_SUB(NOW(), INTERVAL 47520 MINUTE)),
(1830000000000000004, 1796119683661783042, 1800000000000000002, 1, 1, DATE_SUB(NOW(), INTERVAL 7200 MINUTE)),
(1830000000000000005, 1795000000000000106, 1800000000000000002, 2, 1, DATE_SUB(NOW(), INTERVAL 7200 MINUTE)),
(1830000000000000006, 1795000000000000105, 1800000000000000002, 3, 1, DATE_SUB(NOW(), INTERVAL 7200 MINUTE)),
(1830000000000000007, 1795000000000000111, 1800000000000000003, 1, 1, DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(1830000000000000008, 1795000000000000109, 1800000000000000003, 2, 1, DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(1830000000000000009, 1795000000000000110, 1800000000000000003, 3, 1, DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(1830000000000000010, 1795000000000000113, 1800000000000000003, 4, 1, DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(1830000000000000011, 2102360449353351170, 1800000000000000004, 1, 1, DATE_SUB(NOW(), INTERVAL 0 MINUTE)),
(1830000000000000012, 1795000000000000108, 1800000000000000004, 2, 1, DATE_SUB(NOW(), INTERVAL 0 MINUTE)),
(1830000000000000013, 1795000000000000112, 1800000000000000004, 3, 1, DATE_SUB(NOW(), INTERVAL 0 MINUTE)),
(1830000000000000014, 1795000000000000114, 1800000000000000004, 4, 1, DATE_SUB(NOW(), INTERVAL 0 MINUTE)),
(1830000000000000015, 1795000000000000115, 1800000000000000004, 5, 1, DATE_SUB(NOW(), INTERVAL 0 MINUTE)),
(1830000000000000016, 2102360449353351170, 1800000000000000005, 1, 1, DATE_ADD(NOW(), INTERVAL 10080 MINUTE));

-- 提交记录（竞赛提交与练习提交；AC 为参考解，WA 为默认模板，CE 为缺分号）
INSERT IGNORE INTO `tb_user_submit` (`submit_id`, `user_id`, `question_id`, `exam_id`, `program_type`, `user_code`, `pass`, `exe_message`, `score`, `judge_status`, `pass_count`, `total_count`, `time_cost`, `fail_case_id`, `fail_output`, `case_states`, `create_by`, `create_time`) VALUES
(1820000000000000001, 1700000000000000002, 1794933791345602562, 1800000000000000001, 0, 'public int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> seen = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        Integer j = seen.get(target - nums[i]);\n        if (j != null) {\n            return new int[]{j, i};\n        }\n        seen.put(nums[i], i);\n    }\n    return new int[0];\n}', 1, '', 100, 1, 5, 5, 30, NULL, NULL, '11111', 1700000000000000002, DATE_SUB(NOW(), INTERVAL 43188 MINUTE)),
(1820000000000000002, 1700000000000000002, 1794900876543210003, 1800000000000000001, 0, 'public boolean isValid(String s) {\n    Deque<Character> stack = new ArrayDeque<>();\n    for (char c : s.toCharArray()) {\n        if (c == ''('') stack.push('')'');\n        else if (c == ''['') stack.push('']'');\n        else if (c == ''{'') stack.push(''}'');\n        else if (stack.isEmpty() || stack.pop() != c) return false;\n    }\n    return stack.isEmpty();\n}', 1, '', 100, 1, 8, 8, 56, NULL, NULL, '11111111', 1700000000000000002, DATE_SUB(NOW(), INTERVAL 43175 MINUTE)),
(1820000000000000003, 1700000000000000002, 1795000000000000104, 1800000000000000001, 0, 'public int climbStairs(int n) {\n    int a = 1, b = 1;\n    for (int i = 2; i <= n; i++) {\n        int c = a + b;\n        a = b;\n        b = c;\n    }\n    return b;\n}', 1, '', 100, 1, 6, 6, 75, NULL, NULL, '111111', 1700000000000000002, DATE_SUB(NOW(), INTERVAL 43169 MINUTE)),
(1820000000000000004, 1700000000000000001, 1794933791345602562, 1800000000000000001, 0, 'public int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> seen = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        Integer j = seen.get(target - nums[i]);\n        if (j != null) {\n            return new int[]{j, i};\n        }\n        seen.put(nums[i], i);\n    }\n    return new int[0];\n}', 1, '', 100, 1, 5, 5, 49, NULL, NULL, '11111', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 43185 MINUTE)),
(1820000000000000005, 1700000000000000001, 1794900876543210003, 1800000000000000001, 0, 'public boolean isValid(String s) {\n    // 请在此处编写你的代码\n    return false;\n}', 0, '', 75, 2, 6, 8, 47, NULL, '0', '1111110-', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 43170 MINUTE)),
(1820000000000000006, 1700000000000000001, 1794900876543210003, 1800000000000000001, 0, 'public boolean isValid(String s) {\n    Deque<Character> stack = new ArrayDeque<>();\n    for (char c : s.toCharArray()) {\n        if (c == ''('') stack.push('')'');\n        else if (c == ''['') stack.push('']'');\n        else if (c == ''{'') stack.push(''}'');\n        else if (stack.isEmpty() || stack.pop() != c) return false;\n    }\n    return stack.isEmpty();\n}', 1, '', 100, 1, 8, 8, 43, NULL, NULL, '11111111', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 43159 MINUTE)),
(1820000000000000007, 1700000000000000001, 1795000000000000104, 1800000000000000001, 0, 'public int climbStairs(int n) {\n    // 请在此处编写你的代码\n    return 0;\n}', 0, '', 67, 2, 4, 6, 59, NULL, '0', '11110-', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 43130 MINUTE)),
(1820000000000000008, 1700000000000000003, 1794933791345602562, 1800000000000000001, 0, 'public int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> seen = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        Integer j = seen.get(target - nums[i]);\n        if (j != null) {\n            return new int[]{j, i};\n        }\n        seen.put(nums[i], i);\n    }\n    return new int[0];\n}', 1, '', 100, 1, 5, 5, 90, NULL, NULL, '11111', 1700000000000000003, DATE_SUB(NOW(), INTERVAL 43180 MINUTE)),
(1820000000000000009, 1700000000000000003, 1794900876543210003, 1800000000000000001, 0, 'public boolean isValid(String s) {\n    int broken = 1\n    return false;\n}', 0, '编译错误：Solution.java:4: 错误: 需要'';''', 0, 5, 0, 8, NULL, NULL, NULL, '--------', 1700000000000000003, DATE_SUB(NOW(), INTERVAL 43150 MINUTE)),
(1820000000000000010, 1700000000000000003, 1795000000000000104, 1800000000000000001, 0, 'public int climbStairs(int n) {\n    int a = 1, b = 1;\n    for (int i = 2; i <= n; i++) {\n        int c = a + b;\n        a = b;\n        b = c;\n    }\n    return b;\n}', 1, '', 100, 1, 6, 6, 38, NULL, NULL, '111111', 1700000000000000003, DATE_SUB(NOW(), INTERVAL 43135 MINUTE)),
(1820000000000000011, 1700000000000000004, 1794933791345602562, 1800000000000000001, 0, 'public int[] twoSum(int[] nums, int target) {\n    // 请在此处编写你的代码\n    return new int[0];\n}', 0, '', 60, 2, 3, 5, 58, NULL, '0', '1110-', 1700000000000000004, DATE_SUB(NOW(), INTERVAL 43182 MINUTE)),
(1820000000000000012, 1700000000000000004, 1794900876543210003, 1800000000000000001, 0, 'public boolean isValid(String s) {\n    Deque<Character> stack = new ArrayDeque<>();\n    for (char c : s.toCharArray()) {\n        if (c == ''('') stack.push('')'');\n        else if (c == ''['') stack.push('']'');\n        else if (c == ''{'') stack.push(''}'');\n        else if (stack.isEmpty() || stack.pop() != c) return false;\n    }\n    return stack.isEmpty();\n}', 1, '', 100, 1, 8, 8, 93, NULL, NULL, '11111111', 1700000000000000004, DATE_SUB(NOW(), INTERVAL 43145 MINUTE)),
(1820000000000000013, 1700000000000000005, 1795000000000000104, 1800000000000000001, 0, 'public int climbStairs(int n) {\n    int a = 1, b = 1;\n    for (int i = 2; i <= n; i++) {\n        int c = a + b;\n        a = b;\n        b = c;\n    }\n    return b;\n}', 1, '', 100, 1, 6, 6, 81, NULL, NULL, '111111', 1700000000000000005, DATE_SUB(NOW(), INTERVAL 43110 MINUTE)),
(1820000000000000014, 1700000000000000001, 1796119683661783042, 1800000000000000002, 0, 'public boolean isPalindrome(int x) {\n    if (x < 0 || (x % 10 == 0 && x != 0)) return false;\n    int half = 0;\n    while (x > half) {\n        half = half * 10 + x % 10;\n        x /= 10;\n    }\n    return x == half || x == half / 10;\n}', 1, '', 100, 1, 7, 7, 60, NULL, NULL, '1111111', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 2870 MINUTE)),
(1820000000000000015, 1700000000000000001, 1795000000000000106, 1800000000000000002, 0, 'public int search(int[] nums, int target) {\n    int lo = 0, hi = nums.length - 1;\n    while (lo <= hi) {\n        int mid = (lo + hi) >>> 1;\n        if (nums[mid] == target) return mid;\n        if (nums[mid] < target) lo = mid + 1; else hi = mid - 1;\n    }\n    return -1;\n}', 1, '', 100, 1, 5, 5, 99, NULL, NULL, '11111', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 2845 MINUTE)),
(1820000000000000016, 1700000000000000003, 1796119683661783042, 1800000000000000002, 0, 'public boolean isPalindrome(int x) {\n    // 请在此处编写你的代码\n    return false;\n}', 0, '', 71, 2, 5, 7, 95, NULL, '0', '111110-', 1700000000000000003, DATE_SUB(NOW(), INTERVAL 2858 MINUTE)),
(1820000000000000017, 1700000000000000003, 1795000000000000105, 1800000000000000002, 0, 'public int singleNumber(int[] nums) {\n    int x = 0;\n    for (int v : nums) x ^= v;\n    return x;\n}', 1, '', 100, 1, 5, 5, 34, NULL, NULL, '11111', 1700000000000000003, DATE_SUB(NOW(), INTERVAL 2832 MINUTE)),
(1820000000000000018, 1700000000000000004, 1795000000000000106, 1800000000000000002, 0, 'public int search(int[] nums, int target) {\n    int lo = 0, hi = nums.length - 1;\n    while (lo <= hi) {\n        int mid = (lo + hi) >>> 1;\n        if (nums[mid] == target) return mid;\n        if (nums[mid] < target) lo = mid + 1; else hi = mid - 1;\n    }\n    return -1;\n}', 1, '', 100, 1, 5, 5, 83, NULL, NULL, '11111', 1700000000000000004, DATE_SUB(NOW(), INTERVAL 2820 MINUTE)),
(1820000000000000019, 1700000000000000002, 1795000000000000111, 1800000000000000003, 0, 'public int maxSubArray(int[] nums) {\n    int best = nums[0], cur = 0;\n    for (int v : nums) {\n        cur = Math.max(cur + v, v);\n        best = Math.max(best, cur);\n    }\n    return best;\n}', 1, '', 200, 1, 5, 5, 42, NULL, NULL, '11111', 1700000000000000002, DATE_SUB(NOW(), INTERVAL 1350 MINUTE)),
(1820000000000000020, 1700000000000000006, 1795000000000000110, 1800000000000000003, 0, 'public int coinChange(int[] coins, int amount) {\n    // 请在此处编写你的代码\n    return -1;\n}', 0, '', 120, 2, 3, 5, 32, NULL, '0', '1110-', 1700000000000000006, DATE_SUB(NOW(), INTERVAL 1240 MINUTE)),
(1820000000000000021, 1700000000000000001, 1794933791345602562, NULL, 0, 'public int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> seen = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        Integer j = seen.get(target - nums[i]);\n        if (j != null) {\n            return new int[]{j, i};\n        }\n        seen.put(nums[i], i);\n    }\n    return new int[0];\n}', 1, '', 100, 1, 5, 5, 93, NULL, NULL, '11111', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 28800 MINUTE)),
(1820000000000000022, 1700000000000000001, 1796119683661783042, NULL, 0, 'public boolean isPalindrome(int x) {\n    if (x < 0 || (x % 10 == 0 && x != 0)) return false;\n    int half = 0;\n    while (x > half) {\n        half = half * 10 + x % 10;\n        x /= 10;\n    }\n    return x == half || x == half / 10;\n}', 1, '', 100, 1, 7, 7, 40, NULL, NULL, '1111111', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 27360 MINUTE)),
(1820000000000000023, 1700000000000000001, 1795000000000000111, NULL, 0, 'public int maxSubArray(int[] nums) {\n    // 请在此处编写你的代码\n    return 0;\n}', 0, '', 80, 2, 2, 5, 108, NULL, '0', '110--', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 4320 MINUTE)),
(1820000000000000024, 1700000000000000001, 2102360449353351170, NULL, 0, 'public int numIslands(char[][] grid) {\n    int count = 0;\n    for (int i = 0; i < grid.length; i++) {\n        for (int j = 0; j < grid[i].length; j++) {\n            if (grid[i][j] == ''1'') {\n                count++;\n                sink(grid, i, j);\n            }\n        }\n    }\n    return count;\n}\n\nprivate void sink(char[][] g, int i, int j) {\n    if (i < 0 || j < 0 || i >= g.length || j >= g[i].length || g[i][j] != ''1'') return;\n    g[i][j] = ''0'';\n    sink(g, i + 1, j);\n    sink(g, i - 1, j);\n    sink(g, i, j + 1);\n    sink(g, i, j - 1);\n}', 1, '', 200, 1, 5, 5, 36, NULL, NULL, '11111', 1700000000000000001, DATE_SUB(NOW(), INTERVAL 2880 MINUTE)),
(1820000000000000025, 1700000000000000002, 1795000000000000112, NULL, 0, 'public int trap(int[] h) {\n    int l = 0, r = h.length - 1, lm = 0, rm = 0, water = 0;\n    while (l < r) {\n        if (h[l] < h[r]) {\n            lm = Math.max(lm, h[l]);\n            water += lm - h[l++];\n        } else {\n            rm = Math.max(rm, h[r]);\n            water += rm - h[r--];\n        }\n    }\n    return water;\n}', 1, '', 300, 1, 5, 5, 55, NULL, NULL, '11111', 1700000000000000002, DATE_SUB(NOW(), INTERVAL 21600 MINUTE)),
(1820000000000000026, 1700000000000000002, 1795000000000000113, NULL, 0, 'public int minDistance(String a, String b) {\n    int[][] dp = new int[a.length() + 1][b.length() + 1];\n    for (int i = 0; i <= a.length(); i++) dp[i][0] = i;\n    for (int j = 0; j <= b.length(); j++) dp[0][j] = j;\n    for (int i = 1; i <= a.length(); i++) {\n        for (int j = 1; j <= b.length(); j++) {\n            int same = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;\n            dp[i][j] = Math.min(dp[i - 1][j - 1] + same, Math.min(dp[i - 1][j], dp[i][j - 1]) + 1);\n        }\n    }\n    return dp[a.length()][b.length()];\n}', 1, '', 300, 1, 5, 5, 51, NULL, NULL, '11111', 1700000000000000002, DATE_SUB(NOW(), INTERVAL 17280 MINUTE)),
(1820000000000000027, 1700000000000000002, 1795000000000000115, NULL, 0, 'public int totalNQueens(int n) {\n    return place(n, 0, 0, 0, 0);\n}\n\nprivate int place(int n, int row, int cols, int d1, int d2) {\n    if (row == n) return 1;\n    int count = 0;\n    int free = ~(cols | d1 | d2) & ((1 << n) - 1);\n    while (free != 0) {\n        int bit = free & -free;\n        free -= bit;\n        count += place(n, row + 1, cols | bit, (d1 | bit) << 1, (d2 | bit) >> 1);\n    }\n    return count;\n}', 1, '', 300, 1, 5, 5, 93, NULL, NULL, '11111', 1700000000000000002, DATE_SUB(NOW(), INTERVAL 11520 MINUTE)),
(1820000000000000028, 1700000000000000002, 1795000000000000108, NULL, 0, 'public int lengthOfLongestSubstring(String s) {\n    int[] last = new int[128];\n    Arrays.fill(last, -1);\n    int best = 0, left = 0;\n    for (int i = 0; i < s.length(); i++) {\n        char c = s.charAt(i);\n        left = Math.max(left, last[c] + 1);\n        last[c] = i;\n        best = Math.max(best, i - left + 1);\n    }\n    return best;\n}', 1, '', 200, 1, 7, 7, 115, NULL, NULL, '1111111', 1700000000000000002, DATE_SUB(NOW(), INTERVAL 8640 MINUTE)),
(1820000000000000029, 1700000000000000003, 1795000000000000104, NULL, 0, 'public int climbStairs(int n) {\n    int a = 1, b = 1;\n    for (int i = 2; i <= n; i++) {\n        int c = a + b;\n        a = b;\n        b = c;\n    }\n    return b;\n}', 1, '', 100, 1, 6, 6, 31, NULL, NULL, '111111', 1700000000000000003, DATE_SUB(NOW(), INTERVAL 14400 MINUTE)),
(1820000000000000030, 1700000000000000003, 1795000000000000106, NULL, 0, 'public int search(int[] nums, int target) {\n    int lo = 0, hi = nums.length - 1;\n    while (lo <= hi) {\n        int mid = (lo + hi) >>> 1;\n        if (nums[mid] == target) return mid;\n        if (nums[mid] < target) lo = mid + 1; else hi = mid - 1;\n    }\n    return -1;\n}', 1, '', 100, 1, 5, 5, 99, NULL, NULL, '11111', 1700000000000000003, DATE_SUB(NOW(), INTERVAL 12960 MINUTE)),
(1820000000000000031, 1700000000000000005, 1795000000000000110, NULL, 0, 'public int coinChange(int[] coins, int amount) {\n    int broken = 1\n    return -1;\n}', 0, '编译错误：Solution.java:4: 错误: 需要'';''', 0, 5, 0, 5, NULL, NULL, NULL, '-----', 1700000000000000005, DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(1820000000000000032, 1700000000000000007, 1794933791345602562, NULL, 0, 'public int[] twoSum(int[] nums, int target) {\n    // 请在此处编写你的代码\n    return new int[0];\n}', 0, '', 20, 2, 1, 5, 45, NULL, '0', '10---', 1700000000000000007, DATE_SUB(NOW(), INTERVAL 1440 MINUTE));

-- 竞赛报名（已结算竞赛写入得分与排名，其余由结算任务写入）
INSERT IGNORE INTO `tb_user_exam` (`user_exam_id`, `user_id`, `exam_id`, `score`, `exam_rank`, `create_by`, `create_time`) VALUES
(1810000000000000001, 1700000000000000002, 1800000000000000001, 300, 1, 1700000000000000002, DATE_SUB(NOW(), INTERVAL 44610 MINUTE)),
(1810000000000000002, 1700000000000000001, 1800000000000000001, 267, 2, 1700000000000000001, DATE_SUB(NOW(), INTERVAL 44640 MINUTE)),
(1810000000000000003, 1700000000000000003, 1800000000000000001, 200, 3, 1700000000000000003, DATE_SUB(NOW(), INTERVAL 44580 MINUTE)),
(1810000000000000004, 1700000000000000004, 1800000000000000001, 160, 4, 1700000000000000004, DATE_SUB(NOW(), INTERVAL 44550 MINUTE)),
(1810000000000000005, 1700000000000000005, 1800000000000000001, 100, 5, 1700000000000000005, DATE_SUB(NOW(), INTERVAL 44520 MINUTE)),
(1810000000000000006, 1700000000000000001, 1800000000000000002, NULL, NULL, 1700000000000000001, DATE_SUB(NOW(), INTERVAL 4320 MINUTE)),
(1810000000000000007, 1700000000000000003, 1800000000000000002, NULL, NULL, 1700000000000000003, DATE_SUB(NOW(), INTERVAL 4260 MINUTE)),
(1810000000000000008, 1700000000000000004, 1800000000000000002, NULL, NULL, 1700000000000000004, DATE_SUB(NOW(), INTERVAL 4230 MINUTE)),
(1810000000000000009, 1700000000000000002, 1800000000000000002, NULL, NULL, 1700000000000000002, DATE_SUB(NOW(), INTERVAL 4290 MINUTE)),
(1810000000000000010, 1700000000000000002, 1800000000000000003, NULL, NULL, 1700000000000000002, DATE_SUB(NOW(), INTERVAL 2850 MINUTE)),
(1810000000000000011, 1700000000000000006, 1800000000000000003, NULL, NULL, 1700000000000000006, DATE_SUB(NOW(), INTERVAL 2730 MINUTE)),
(1810000000000000012, 1700000000000000001, 1800000000000000003, NULL, NULL, 1700000000000000001, DATE_SUB(NOW(), INTERVAL 2880 MINUTE)),
(1810000000000000013, 1700000000000000003, 1800000000000000003, NULL, NULL, 1700000000000000003, DATE_SUB(NOW(), INTERVAL 2820 MINUTE)),
(1810000000000000014, 1700000000000000002, 1800000000000000004, NULL, NULL, 1700000000000000002, DATE_ADD(NOW(), INTERVAL 2910 MINUTE)),
(1810000000000000015, 1700000000000000005, 1800000000000000004, NULL, NULL, 1700000000000000005, DATE_ADD(NOW(), INTERVAL 3000 MINUTE));

-- 站内消息（系统通知 + 已结算竞赛的排名通知）
INSERT IGNORE INTO `tb_message_text` (`text_id`, `message_type`, `message_title`, `message_content`, `create_by`, `create_time`) VALUES
(1900000000000000001, 1, '欢迎来到墨衡 OJ', '题库、竞赛与 AI 做题辅导均已开放，祝你刷题愉快。', 0, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1900000000000000002, 2, '竞赛结果通知', '您参与的竞赛：新手入门周赛 · 第 1 期：本次共参赛5人，您排名：第1名！', 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE)),
(1900000000000000003, 2, '竞赛结果通知', '您参与的竞赛：新手入门周赛 · 第 1 期：本次共参赛5人，您排名：第2名！', 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE)),
(1900000000000000004, 2, '竞赛结果通知', '您参与的竞赛：新手入门周赛 · 第 1 期：本次共参赛5人，您排名：第3名！', 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE)),
(1900000000000000005, 2, '竞赛结果通知', '您参与的竞赛：新手入门周赛 · 第 1 期：本次共参赛5人，您排名：第4名！', 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE)),
(1900000000000000006, 2, '竞赛结果通知', '您参与的竞赛：新手入门周赛 · 第 1 期：本次共参赛5人，您排名：第5名！', 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE));
INSERT IGNORE INTO `tb_message` (`message_id`, `text_id`, `send_id`, `rec_id`, `is_read`, `create_by`, `create_time`) VALUES
(1910000000000000001, 1900000000000000001, 0, 1700000000000000001, 1, 0, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1910000000000000002, 1900000000000000001, 0, 1700000000000000002, 1, 0, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1910000000000000003, 1900000000000000001, 0, 1700000000000000003, 0, 0, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1910000000000000004, 1900000000000000001, 0, 1700000000000000004, 0, 0, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1910000000000000005, 1900000000000000001, 0, 1700000000000000005, 0, 0, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1910000000000000006, 1900000000000000001, 0, 1700000000000000006, 0, 0, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1910000000000000007, 1900000000000000001, 0, 1700000000000000007, 0, 0, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(1910000000000000008, 1900000000000000002, 0, 1700000000000000002, 1, 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE)),
(1910000000000000009, 1900000000000000003, 0, 1700000000000000001, 0, 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE)),
(1910000000000000010, 1900000000000000004, 0, 1700000000000000003, 0, 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE)),
(1910000000000000011, 1900000000000000005, 0, 1700000000000000004, 0, 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE)),
(1910000000000000012, 1900000000000000006, 0, 1700000000000000005, 0, 0, DATE_SUB(NOW(), INTERVAL 43050 MINUTE));

-- =============================================================
-- 三、XXL-JOB 调度库（xxl-job-admin 2.4.0 官方表结构 + 本项目执行器与任务）
-- =============================================================

CREATE DATABASE IF NOT EXISTS `xxl_job` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `xxl_job`;

CREATE TABLE IF NOT EXISTS `xxl_job_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `job_group` int(11) NOT NULL COMMENT '执行器主键ID',
  `job_desc` varchar(255) NOT NULL,
  `add_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `author` varchar(64) DEFAULT NULL COMMENT '作者',
  `alarm_email` varchar(255) DEFAULT NULL COMMENT '报警邮件',
  `schedule_type` varchar(50) NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
  `schedule_conf` varchar(128) DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
  `misfire_strategy` varchar(50) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
  `executor_route_strategy` varchar(50) DEFAULT NULL COMMENT '执行器路由策略',
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '执行器任务handler',
  `executor_param` varchar(512) DEFAULT NULL COMMENT '执行器任务参数',
  `executor_block_strategy` varchar(50) DEFAULT NULL COMMENT '阻塞处理策略',
  `executor_timeout` int(11) NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
  `executor_fail_retry_count` int(11) NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `glue_type` varchar(50) NOT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) DEFAULT NULL COMMENT 'GLUE备注',
  `glue_updatetime` datetime DEFAULT NULL COMMENT 'GLUE更新时间',
  `child_jobid` varchar(255) DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
  `trigger_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
  `trigger_last_time` bigint(13) NOT NULL DEFAULT '0' COMMENT '上次调度时间',
  `trigger_next_time` bigint(13) NOT NULL DEFAULT '0' COMMENT '下次调度时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `job_group` int(11) NOT NULL COMMENT '执行器主键ID',
  `job_id` int(11) NOT NULL COMMENT '任务，主键ID',
  `executor_address` varchar(255) DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '执行器任务handler',
  `executor_param` varchar(512) DEFAULT NULL COMMENT '执行器任务参数',
  `executor_sharding_param` varchar(20) DEFAULT NULL COMMENT '执行器任务分片参数，格式如 1/2',
  `executor_fail_retry_count` int(11) NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `trigger_time` datetime DEFAULT NULL COMMENT '调度-时间',
  `trigger_code` int(11) NOT NULL COMMENT '调度-结果',
  `trigger_msg` text COMMENT '调度-日志',
  `handle_time` datetime DEFAULT NULL COMMENT '执行-时间',
  `handle_code` int(11) NOT NULL COMMENT '执行-状态',
  `handle_msg` text COMMENT '执行-日志',
  `alarm_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
  PRIMARY KEY (`id`),
  KEY `I_trigger_time` (`trigger_time`),
  KEY `I_handle_code` (`handle_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_log_report` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `trigger_day` datetime DEFAULT NULL COMMENT '调度-时间',
  `running_count` int(11) NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
  `suc_count` int(11) NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
  `fail_count` int(11) NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_logglue` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `job_id` int(11) NOT NULL COMMENT '任务，主键ID',
  `glue_type` varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
  `add_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_registry` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `registry_group` varchar(50) NOT NULL,
  `registry_key` varchar(255) NOT NULL,
  `registry_value` varchar(255) NOT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `i_g_k_v` (`registry_group`,`registry_key`,`registry_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_name` varchar(64) NOT NULL COMMENT '执行器AppName',
  `title` varchar(12) NOT NULL COMMENT '执行器名称',
  `address_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
  `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '账号',
  `password` varchar(50) NOT NULL COMMENT '密码',
  `role` tinyint(4) NOT NULL COMMENT '角色：0-普通用户、1-管理员',
  `permission` varchar(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_lock` (
  `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
  PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `xxl_job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`) VALUES
(1, 'xxl-job-executor-sample', '示例执行器', 0, NULL, NOW()),
(2, 'oj-job-executor', '墨衡OJ定时任务', 0, NULL, NOW());

INSERT IGNORE INTO `xxl_job_info` (`id`, `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`) VALUES
(1, 1, '测试任务1', NOW(), NOW(), 'XXL', '', 'CRON', '0 0 0 * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 0),
(2, 2, '刷新竞赛列表缓存（未完赛/已完赛）', NOW(), NOW(), '墨衡', '', 'CRON', '0 */10 * * * ?', 'DO_NOTHING', 'FIRST', 'examListOrganizeHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1),
(3, 2, '结算已结束竞赛的排名', NOW(), NOW(), '墨衡', '', 'CRON', '0 */5 * * * ?', 'DO_NOTHING', 'FIRST', 'examRankSettlementHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1);

-- 调度中心账号 admin / 123456（xxl-job 默认 MD5）
INSERT IGNORE INTO `xxl_job_user` (`id`, `username`, `password`, `role`, `permission`) VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL);
INSERT IGNORE INTO `xxl_job_lock` (`lock_name`) VALUES ('schedule_lock');

