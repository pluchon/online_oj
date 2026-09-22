-- -------------------------------------------------------------
-- 做题代码草稿表（归属 oj-friend）：每个用户每道题一份，跨设备保存未提交的代码
-- -------------------------------------------------------------
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS tb_user_code_draft (
    draft_id BIGINT UNSIGNED NOT NULL COMMENT '草稿id(主键)',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户id',
    question_id BIGINT UNSIGNED NOT NULL COMMENT '题目id',
    code TEXT NOT NULL COMMENT '代码内容',
    create_by BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (draft_id),
    UNIQUE KEY uk_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='做题代码草稿表';
