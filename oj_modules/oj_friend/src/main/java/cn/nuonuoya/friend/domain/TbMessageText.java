package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 消息正文实体
@TableName("tb_message_text")
@Getter
@Setter
@ToString
public class TbMessageText extends BaseEntity {

    // 消息内容ID（主键，雪花算法）
    @TableId(value = "TEXT_ID", type = IdType.ASSIGN_ID)
    private Long textId;

    // 消息标题
    private String messageTitle;

    // 消息内容
    private String messageContent;
}
