package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// C端学员解题日历查询DTO
@Getter
@Setter
@ToString
@Schema(description = "C端学员解题日历查询入参")
public class UserCalendarQueryDTO {

    // 查询年份（默认当前自然年，如 2026）
    @Schema(description = "查询年份（默认当前自然年，如 2026）")
    private Integer year;
}
