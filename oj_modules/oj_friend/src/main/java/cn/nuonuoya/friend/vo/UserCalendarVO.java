package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

// C端学员解题日历视图对象
@Getter
@Setter
@ToString
@Schema(description = "C端学员解题日历响应")
public class UserCalendarVO implements Serializable {

    // 年份
    @Schema(description = "年份")
    private Integer year;

    // 该自然年内总提交数
    @Schema(description = "该自然年内总提交数")
    private Integer totalSubmissions;

    // 每日提交统计列表
    @Schema(description = "每日提交统计列表")
    private List<UserCalendarItemVO> calendarData;
}
