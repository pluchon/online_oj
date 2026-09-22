-- -------------------------------------------------------------
-- AI 做题辅导对话表（归属 oj-friend）
-- 会话：每个用户每道题一个；消息：用户提问与 AI 回复，同时用于每日次数统计与审计
-- -------------------------------------------------------------
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS tb_ai_chat_session (
    session_id BIGINT UNSIGNED NOT NULL COMMENT '会话id(主键)',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户id',
    question_id BIGINT UNSIGNED NOT NULL COMMENT '题目id',
    create_by BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (session_id),
    UNIQUE KEY uk_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 做题辅导会话表';

CREATE TABLE IF NOT EXISTS tb_ai_chat_message (
    message_id BIGINT UNSIGNED NOT NULL COMMENT '消息id(主键)',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '会话id',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户id',
    role TINYINT UNSIGNED NOT NULL COMMENT '角色(1:用户 2:AI)',
    action TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '提问类型(0:自由提问 1:思路 2:分析提交 3:编译错误 4:代码点评)',
    content TEXT NOT NULL COMMENT '消息内容',
    model VARCHAR(64) DEFAULT NULL COMMENT '生成回复的模型(仅 AI 消息)',
    prompt_tokens INT UNSIGNED DEFAULT NULL COMMENT '输入 Token 数(仅 AI 消息)',
    completion_tokens INT UNSIGNED DEFAULT NULL COMMENT '输出 Token 数(仅 AI 消息)',
    create_by BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (message_id),
    KEY idx_session_time (session_id, create_time),
    KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 做题辅导消息表';
