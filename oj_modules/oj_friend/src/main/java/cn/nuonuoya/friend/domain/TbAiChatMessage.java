package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// AI 做题辅导消息实体
@TableName("tb_ai_chat_message")
@Getter
@Setter
@ToString
public class TbAiChatMessage extends BaseEntity {

    // 消息id（雪花算法）
    @TableId(value = "MESSAGE_ID", type = IdType.ASSIGN_ID)
    private Long messageId;

    // 会话id
    private Long sessionId;

    // 用户id
    private Long userId;

    // 角色（见 AiChatRoleEnum）
    private Integer role;

    // 提问类型（见 AiTutorActionEnum）
    private Integer action;

    // 消息内容
    private String content;

    // 生成回复的模型（仅 AI 消息）
    private String model;

    // 输入 Token 数（仅 AI 消息）
    private Integer promptTokens;

    // 输出 Token 数（仅 AI 消息）
    private Integer completionTokens;
}
