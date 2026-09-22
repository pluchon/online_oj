package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 站内消息视图对象
@Getter
@Setter
public class MessageVO {

    // 消息投递记录ID
    @Schema(description = "消息ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;

    // 消息内容ID
    @Schema(description = "消息内容ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long textId;

    // 消息类型（1: 系统通知 2: 竞赛通知）
    @Schema(description = "消息类型(1:系统通知 2:竞赛通知)")
    private Integer type;

    // 消息标题
    @Schema(description = "消息标题")
    private String title;

    // 消息内容
    @Schema(description = "消息内容")
    private String content;

    // 发送人ID（0代表系统）
    @Schema(description = "发送人ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sendId;

    // 是否已读：0: 未读, 1: 已读
    @Schema(description = "是否已读(0:未读, 1:已读)")
    private Integer isRead;

    // 发送时间
    @Schema(description = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
