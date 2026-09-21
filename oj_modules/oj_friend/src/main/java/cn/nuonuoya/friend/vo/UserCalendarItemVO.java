package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

// C端学员解题日历每日提交统计项VO
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "每日提交统计项数据")
public class UserCalendarItemVO implements Serializable {

    // 日期（格式：yyyy-MM-dd）
    @Schema(description = "打卡日期（格式：yyyy-MM-dd）")
    private String date;

    // 当天提交次数
    @Schema(description = "当天提交次数")
    private Integer count;
}
