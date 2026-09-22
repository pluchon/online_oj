package cn.nuonuoya.friend.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 站内消息分页查询DTO
@Getter
@Setter
@ToString
@Schema(description = "站内消息分页查询参数")
public class MessageQueryDTO extends PageQuery {

    // 消息类型（1: 系统通知 2: 竞赛通知，不传为全部）
    @Min(value = 1, message = "消息类型不合法")
    @Max(value = 2, message = "消息类型不合法")
    @Schema(description = "消息类型（1: 系统通知 2: 竞赛通知，不传为全部）")
    private Integer type;

    // 关键词（匹配标题或内容）
    @Size(max = 50, message = "关键词不能超过50个字符")
    @Schema(description = "关键词（匹配标题或内容）")
    private String keyword;
}
