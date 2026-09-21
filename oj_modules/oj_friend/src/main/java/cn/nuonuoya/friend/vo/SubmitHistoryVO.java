package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 本题提交记录视图对象
@Getter
@Setter
@Schema(description = "本题提交记录视图对象")
public class SubmitHistoryVO {

    // 提交记录ID
    @Schema(description = "提交记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitId;

    // 是否通过（0: 未通过 1: 通过 2: 评测中）
    @Schema(description = "是否通过 0:未通过 1:通过 2:评测中")
    private Integer pass;

    // 判题状态（1:AC 2:WA 3:TLE 4:MLE 5:CE 6:RE 8:SE）
    @Schema(description = "判题状态")
    private Integer status;

    // 通过用例数
    @Schema(description = "通过用例数")
    private Integer passCount;

    // 总用例数
    @Schema(description = "总用例数")
    private Integer totalCount;

    // 执行耗时（毫秒）
    @Schema(description = "执行耗时(ms)")
    private Integer timeCost;

    // 得分
    @Schema(description = "得分")
    private Integer score;

    // 提交的代码（用于载回编辑器）
    @Schema(description = "提交的代码")
    private String userCode;

    // 提交时间
    @Schema(description = "提交时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
