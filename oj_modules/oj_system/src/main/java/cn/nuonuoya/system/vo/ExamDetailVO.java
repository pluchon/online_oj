package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 竞赛详情视图对象
@Getter
@Setter
@Schema(description = "竞赛详情视图对象")
public class ExamDetailVO {

    // 竞赛ID（序列化为字符串防止JS前端丢失精度）
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

    // 是否发布（0: 未发布 1: 已发布）
    @Schema(description = "是否发布（0: 未发布 1: 已发布）")
    private Integer status;

    // 发布状态描述（未发布/已发布）
    @Schema(description = "发布状态描述")
    private String statusDesc;

    // 创建人昵称
    @Schema(description = "创建人")
    private String creatorName;

    // 创建时间
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
