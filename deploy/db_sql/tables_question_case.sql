-- ----------------------------
-- 题目测试用例表 + 判题改造增量脚本（可重复执行）
-- 执行顺序：int.sql、tables_user_submit.sql 之后
-- ----------------------------

-- 1. 题目测试用例表（公开示例与隐藏用例共表，is_sample 区分）
CREATE TABLE IF NOT EXISTS `tb_question_case` (
  `case_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用例id',
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
  KEY `idx_question_sample` (`question_id`, `is_sample`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目测试用例表';

-- 2. main 函数改为从标准输入读取用例，长度放宽
ALTER TABLE `tb_question` MODIFY COLUMN `main_func` text NOT NULL COMMENT 'main函数（首行读用例数，逐用例读参数并每行输出一个结果）';

-- 3. 提交记录补充判题明细字段
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user_submit' AND COLUMN_NAME = 'judge_status');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `tb_user_submit`
     ADD COLUMN `judge_status` tinyint DEFAULT NULL COMMENT ''判题状态 1:AC 2:WA 3:TLE 4:MLE 5:CE 6:RE 8:SE'' AFTER `score`,
     ADD COLUMN `pass_count` int NOT NULL DEFAULT ''0'' COMMENT ''通过用例数'' AFTER `judge_status`,
     ADD COLUMN `total_count` int NOT NULL DEFAULT ''0'' COMMENT ''总用例数'' AFTER `pass_count`,
     ADD COLUMN `time_cost` int DEFAULT NULL COMMENT ''执行耗时(ms)'' AFTER `total_count`,
     ADD COLUMN `fail_case_id` bigint unsigned DEFAULT NULL COMMENT ''首个未通过用例id'' AFTER `time_cost`,
     ADD COLUMN `fail_output` varchar(2000) DEFAULT NULL COMMENT ''首个未通过用例的实际输出'' AFTER `fail_case_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.1 提交记录逐用例状态（按用例顺序，1: 通过 0: 未通过 -: 未执行）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user_submit' AND COLUMN_NAME = 'case_states');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `tb_user_submit` ADD COLUMN `case_states` varchar(500) DEFAULT NULL COMMENT ''逐用例状态 1:通过 0:未通过 -:未执行'' AFTER `fail_output`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 三道初始题目的 main 函数（标准输入驱动）
UPDATE `tb_question` SET `main_func` = 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        String s = in.readLine();\n        System.out.println(m.isValid(s));\n    }\n}'
WHERE `question_id` = 1794900876543210003;

UPDATE `tb_question` SET `main_func` = 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int[] nums = Arrays.stream(in.readLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();\n        int target = Integer.parseInt(in.readLine().trim());\n        int[] r = m.twoSum(nums, target);\n        if (r == null) {\n            System.out.println("null");\n        } else {\n            Arrays.sort(r);\n            System.out.println(Arrays.toString(r).replace(" ", ""));\n        }\n    }\n}'
WHERE `question_id` = 1794933791345602562;

UPDATE `tb_question` SET `main_func` = 'public static void main(String[] args) throws IOException {\n    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));\n    int t = Integer.parseInt(in.readLine().trim());\n    Main m = new Main();\n    for (int i = 0; i < t; i++) {\n        int x = Integer.parseInt(in.readLine().trim());\n        System.out.println(m.isPalindrome(x));\n    }\n}'
WHERE `question_id` = 1796119683661783042;

-- 5. 三道初始题目的用例（该题尚无有效用例时才写入）
INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1794900876543210003 AS q, 's = "()"' AS di, 'true' AS do_, '()' AS ji, 'true' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1794900876543210003, 's = "()[]{}"', 'true', '()[]{}', 'true', 1, 2, 1
  UNION ALL SELECT 1794900876543210003, 's = "(]"', 'false', '(]', 'false', 1, 3, 1
  UNION ALL SELECT 1794900876543210003, 's = "([)]"', 'false', '([)]', 'false', 0, 4, 1
  UNION ALL SELECT 1794900876543210003, 's = "{[]}"', 'true', '{[]}', 'true', 0, 5, 1
  UNION ALL SELECT 1794900876543210003, 's = "(("', 'false', '((', 'false', 0, 6, 1
  UNION ALL SELECT 1794900876543210003, 's = "){"', 'false', '){', 'false', 0, 7, 1
  UNION ALL SELECT 1794900876543210003, 's = "[({})]"', 'true', '[({})]', 'true', 0, 8, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1794900876543210003 AND `delete_state` = 0);

INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1794933791345602562 AS q, 'nums = [2,7,11,15], target = 9' AS di, '[0,1]' AS do_, '2,7,11,15\n9' AS ji, '[0,1]' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1794933791345602562, 'nums = [3,2,4], target = 6', '[1,2]', '3,2,4\n6', '[1,2]', 1, 2, 1
  UNION ALL SELECT 1794933791345602562, 'nums = [3,3], target = 6', '[0,1]', '3,3\n6', '[0,1]', 0, 3, 1
  UNION ALL SELECT 1794933791345602562, 'nums = [-1,-2,-3,-4,-5], target = -8', '[2,4]', '-1,-2,-3,-4,-5\n-8', '[2,4]', 0, 4, 1
  UNION ALL SELECT 1794933791345602562, 'nums = [0,4,3,0], target = 0', '[0,3]', '0,4,3,0\n0', '[0,3]', 0, 5, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1794933791345602562 AND `delete_state` = 0);

INSERT INTO `tb_question_case` (`question_id`, `display_input`, `display_output`, `judge_input`, `judge_output`, `is_sample`, `sort_order`, `create_by`)
SELECT * FROM (
  SELECT 1796119683661783042 AS q, 'x = 121' AS di, 'true' AS do_, '121' AS ji, 'true' AS jo, 1 AS s, 1 AS o, 1 AS c
  UNION ALL SELECT 1796119683661783042, 'x = -121', 'false', '-121', 'false', 1, 2, 1
  UNION ALL SELECT 1796119683661783042, 'x = 10', 'false', '10', 'false', 1, 3, 1
  UNION ALL SELECT 1796119683661783042, 'x = 0', 'true', '0', 'true', 0, 4, 1
  UNION ALL SELECT 1796119683661783042, 'x = 12321', 'true', '12321', 'true', 0, 5, 1
  UNION ALL SELECT 1796119683661783042, 'x = 1000021', 'false', '1000021', 'false', 0, 6, 1
  UNION ALL SELECT 1796119683661783042, 'x = 2147447412', 'true', '2147447412', 'true', 0, 7, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `tb_question_case` WHERE `question_id` = 1796119683661783042 AND `delete_state` = 0);
