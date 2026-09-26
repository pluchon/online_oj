-- =============================================================
-- 增量脚本：定时公开已结束竞赛的题目（2026-09-26）
-- 适用：已用旧版 oj_init.sql 初始化过的库；新库直接执行 oj_init.sql 即可，不需要本脚本
-- 内容：XXL-JOB 新增任务「公开已结束竞赛的题目」，每 5 分钟（第 30 秒）执行一次，调用 oj-system 把
--       所在竞赛都已结束的竞赛题改为刷题，题目随后进入 C 端题库
-- 可重复执行：固定主键 INSERT IGNORE
-- =============================================================

SET NAMES utf8mb4;
USE `xxl_job`;

INSERT IGNORE INTO `xxl_job_info` (`id`, `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`) VALUES
(4, 2, '公开已结束竞赛的题目（竞赛题转为刷题）', NOW(), NOW(), '墨衡', '', 'CRON', '30 */5 * * * ?', 'DO_NOTHING', 'FIRST', 'questionPublishHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1);

-- 核对
SELECT `id`, `job_desc`, `schedule_conf`, `executor_handler`, `trigger_status` FROM `xxl_job_info` WHERE `id` = 4;
