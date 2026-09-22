package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 历史对话中的一条消息
@Getter
@Setter
@ToString
public class AiTutorHistoryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 是否为用户消息（否则为 AI 回复）
    private Boolean fromUser;

    // 消息内容
    private String content;
}
