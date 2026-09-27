package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 最近一场竞赛的参与情况视图对象
@Getter
@Setter
public class OverviewExamVO {

    // 竞赛ID
    @Schema(description = "竞赛ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    // 竞赛标题
    @Schema(description = "竞赛标题")
    private String title;

    // 开始时间
    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    // 结束时间
    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // 是否已结束
    @Schema(description = "是否已结束")
    private Boolean finished;

    // 报名人数
    @Schema(description = "报名人数")
    private Integer enrollCount;

    // 实际提交过代码的人数
    @Schema(description = "实际提交过代码的人数")
    private Integer participantCount;
}
