package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// AI 做题辅导会话实体（每个用户每道题一个）
@TableName("tb_ai_chat_session")
@Getter
@Setter
@ToString
public class TbAiChatSession extends BaseEntity {

    // 会话id（雪花算法）
    @TableId(value = "SESSION_ID", type = IdType.ASSIGN_ID)
    private Long sessionId;

    // 用户id
    private Long userId;

    // 题目id
    private Long questionId;
}
