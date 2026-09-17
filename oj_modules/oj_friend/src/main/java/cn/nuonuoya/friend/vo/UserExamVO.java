package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 用户已报名竞赛视图对象（用于“我的竞赛”展示）
@Getter
@Setter
public class UserExamVO {

    // 竞赛ID
    @Schema(description = "竞赛ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    // 竞赛标题
    @Schema(description = "竞赛标题")
    private String title;

    // 竞赛开始时间
    @Schema(description = "竞赛开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    // 竞赛结束时间
    @Schema(description = "竞赛结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // 报名时间
    @Schema(description = "报名时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 竞赛得分（未完赛或未参与时为null）
    @Schema(description = "竞赛得分")
    private Integer score;

    // 竞赛排名（未完赛或未参与时为null）
    @Schema(description = "竞赛排名")
    private Integer examRank;

    // 竞赛动态状态（0:未开赛 1:进行中 2:已完赛）
    @Schema(description = "竞赛动态状态")
    private Integer contestStatus;

    // 竞赛动态状态描述
    @Schema(description = "竞赛动态状态描述")
    private String contestStatusDesc;

    // 是否已报名（在我的竞赛中恒为true）
    @Schema(description = "是否已报名")
    private Boolean isEnter = true;
}
