-- =============================================================
-- 增量脚本：题目官方题解（2026-09-25）
-- 适用：已用旧版 oj_init.sql 初始化过的库；新库直接执行 oj_init.sql 即可，不需要本脚本
-- 可重复执行：建表用 IF NOT EXISTS，示例题解用 INSERT IGNORE（固定主键），且只写入库里仍存在的题目
-- =============================================================

SET NAMES utf8mb4;
USE `bitoj_dev`;

-- 题目官方题解表（一题一篇，已删除的不占位）
CREATE TABLE IF NOT EXISTS `tb_question_editorial` (
  `editorial_id` bigint unsigned NOT NULL COMMENT '题解id(主键)',
  `question_id` bigint unsigned NOT NULL COMMENT '题目id',
  `content` text NOT NULL COMMENT '题解内容(Markdown)',
  `create_by` bigint unsigned NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint unsigned DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `delete_state` tinyint NOT NULL DEFAULT '0' COMMENT '0: 正常 1: 已删除',
  `active_question_id` bigint unsigned GENERATED ALWAYS AS (IF(`delete_state` = 0, `question_id`, NULL)) VIRTUAL COMMENT '未删除题解的题目id(一题一篇的唯一约束用)',
  PRIMARY KEY (`editorial_id`),
  UNIQUE KEY `uk_active_question` (`active_question_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目官方题解表';

-- 示例题解（题目不存在时跳过）
INSERT IGNORE INTO `tb_question_editorial` (`editorial_id`, `question_id`, `content`, `create_by`, `create_time`)
SELECT 1800000000000002001, q.`question_id`, '## 思路\n\n暴力枚举两两组合是 O(n²)。更好的做法是一边遍历一边用哈希表记录「数值 → 下标」：走到 `nums[i]` 时，先查 `target - nums[i]` 是否出现过，出现过就找到了答案；没出现就把 `nums[i]` 放进表里。\n\n先查后放，保证不会用同一个元素两次。\n\n## 复杂度\n\n- 时间：O(n)，每个元素只访问一次\n- 空间：O(n)，哈希表最多存 n 个元素\n\n## 代码\n\n```java\npublic int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> seen = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        Integer j = seen.get(target - nums[i]);\n        if (j != null) {\n            return new int[]{j, i};\n        }\n        seen.put(nums[i], i);\n    }\n    return new int[0];\n}\n```', 1, NOW() FROM `tb_question` q WHERE q.`question_id` = 1794933791345602562;

INSERT IGNORE INTO `tb_question_editorial` (`editorial_id`, `question_id`, `content`, `create_by`, `create_time`)
SELECT 1800000000000002002, q.`question_id`, '## 思路\n\n括号要「后开的先闭」，正好是栈的顺序。遍历字符串：遇到左括号入栈；遇到右括号时，栈顶必须是与之配对的左括号，否则不合法，配对成功就弹出。\n\n遍历结束后栈必须为空，否则还有左括号没闭合。\n\n## 复杂度\n\n- 时间：O(n)\n- 空间：O(n)，最坏情况全是左括号\n\n## 代码\n\n```java\npublic boolean isValid(String s) {\n    Deque<Character> stack = new ArrayDeque<>();\n    for (char c : s.toCharArray()) {\n        if (c == ''('') {\n            stack.push('')'');\n        } else if (c == ''['') {\n            stack.push('']'');\n        } else if (c == ''{'') {\n            stack.push(''}'');\n        } else if (stack.isEmpty() || stack.pop() != c) {\n            return false;\n        }\n    }\n    return stack.isEmpty();\n}\n```', 1, NOW() FROM `tb_question` q WHERE q.`question_id` = 1794900876543210003;

INSERT IGNORE INTO `tb_question_editorial` (`editorial_id`, `question_id`, `content`, `create_by`, `create_time`)
SELECT 1800000000000002003, q.`question_id`, '## 思路\n\n负数一定不是回文；末尾是 0 的正数也不是（开头不可能是 0）。\n\n其余情况不必把整个数反转（可能溢出），只反转后一半：不断把 `x` 的末位移到 `rev` 上，直到 `rev >= x`。此时偶数位要求 `x == rev`，奇数位中间那一位不影响回文，要求 `x == rev / 10`。\n\n## 复杂度\n\n- 时间：O(log x)，每次去掉一位\n- 空间：O(1)\n\n## 代码\n\n```java\npublic boolean isPalindrome(int x) {\n    if (x < 0 || (x % 10 == 0 && x != 0)) {\n        return false;\n    }\n    int rev = 0;\n    while (x > rev) {\n        rev = rev * 10 + x % 10;\n        x /= 10;\n    }\n    return x == rev || x == rev / 10;\n}\n```', 1, NOW() FROM `tb_question` q WHERE q.`question_id` = 1796119683661783042;

-- 核对：有效题解数
SELECT COUNT(*) AS editorial_count FROM `tb_question_editorial` WHERE `delete_state` = 0;
