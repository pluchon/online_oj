package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 竞赛列表项视图对象
@Getter
@Setter
public class ExamVO {

    // 竞赛ID（转为字符串避免前端大数精度丢失）
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

    // 是否发布（0:未发布 1:已发布）
    @Schema(description = "是否发布（0:未发布 1:已发布）")
    private Integer status;

    // 发布状态描述（未发布/已发布）
    @Schema(description = "发布状态描述")
    private String statusDesc;

    // 创建用户昵称（联查用户表获得）
    @Schema(description = "创建用户")
    private String creatorName;

    // 参赛人数（报名该竞赛的用户数）
    @Schema(description = "参赛人数")
    private Long enterCount;

    // 创建时间
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
