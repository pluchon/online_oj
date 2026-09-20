package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 用户消息投递关系实体
@TableName("tb_message")
@Getter
@Setter
@ToString
public class TbMessage extends BaseEntity {

    // 消息投递ID（主键，雪花算法）
    @TableId(value = "MESSAGE_ID", type = IdType.ASSIGN_ID)
    private Long messageId;

    // 关联的消息正文ID
    private Long textId;

    // 消息发送人ID（0代表系统）
    private Long sendId;

    // 消息接收人ID
    private Long recId;

    // 是否已读：0: 未读, 1: 已读
    private Integer isRead;
}
