package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 申诉今日剩余次数视图对象
@Getter
@Setter
@Schema(description = "申诉今日剩余次数")
public class AppealQuotaVO {

    // AI 初审今日剩余次数
    @Schema(description = "AI 初审今日剩余次数")
    private Integer reviewRemaining;

    // AI 初审每日次数
    @Schema(description = "AI 初审每日次数")
    private Integer reviewLimit;

    // 正式申诉今日剩余次数
    @Schema(description = "正式申诉今日剩余次数")
    private Integer appealRemaining;

    // 正式申诉每日次数
    @Schema(description = "正式申诉每日次数")
    private Integer appealLimit;
}
