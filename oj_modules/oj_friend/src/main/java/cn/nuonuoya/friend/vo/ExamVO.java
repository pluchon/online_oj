package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// C端竞赛卡片视图对象
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

    // 发布状态（1:已发布）
    @Schema(description = "发布状态")
    private Integer status;

    // 竞赛动态状态（0:未开赛 1:进行中 2:已完赛）
    @Schema(description = "竞赛动态状态（0:未开赛 1:进行中 2:已完赛）")
    private Integer contestStatus;

    // 竞赛动态状态描述
    @Schema(description = "竞赛动态状态描述")
    private String contestStatusDesc;

    // 卡片操作按钮文案
    @Schema(description = "卡片操作按钮文案")
    private String btnText;

    // 当前登录用户是否已报名该竞赛
    @Schema(description = "当前登录用户是否已报名该竞赛")
    private Boolean isEnter;
}
