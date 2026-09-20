-- -------------------------------------------------------------
-- 消息中心表结构定义
-- 包含消息正文表 (tb_message_text) 与用户消息投递表 (tb_message)
-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS tb_message_text (
    text_id BIGINT UNSIGNED NOT NULL COMMENT '消息内容id(主键)',
    message_title VARCHAR(50) NOT NULL COMMENT '消息标题',
    message_content VARCHAR(500) NOT NULL COMMENT '消息内容',
    create_by BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (text_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息正文表';

CREATE TABLE IF NOT EXISTS tb_message (
    message_id BIGINT UNSIGNED NOT NULL COMMENT '消息id(主键)',
    text_id BIGINT UNSIGNED NOT NULL COMMENT '关联的消息内容id',
    send_id BIGINT UNSIGNED NOT NULL COMMENT '消息发送人id(0代表系统)',
    rec_id BIGINT UNSIGNED NOT NULL COMMENT '消息接收人id',
    is_read TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已读(0:未读, 1:已读)',
    create_by BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (message_id),
    KEY idx_rec_read (rec_id, is_read, create_time),
    KEY idx_text_id (text_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户消息投递表';
