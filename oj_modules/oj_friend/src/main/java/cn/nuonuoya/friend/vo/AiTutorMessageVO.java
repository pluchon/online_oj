package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// AI 辅导消息视图
@Getter
@Setter
@Schema(description = "AI 辅导消息")
public class AiTutorMessageVO {

    // 消息id
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "消息id")
    private Long messageId;

    // 是否为用户消息
    @Schema(description = "是否为用户消息")
    private Boolean fromUser;

    // 提问类型
    @Schema(description = "提问类型")
    private Integer action;

    // 消息内容
    @Schema(description = "消息内容（AI 回复为 Markdown）")
    private String content;

    // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
