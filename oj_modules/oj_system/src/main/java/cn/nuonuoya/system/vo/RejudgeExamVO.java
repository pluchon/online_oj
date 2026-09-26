package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 重判涉及的竞赛视图对象
@Getter
@Setter
public class RejudgeExamVO {

    // 竞赛ID
    @Schema(description = "竞赛ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    // 竞赛标题
    @Schema(description = "竞赛标题")
    private String title;

    // 是否已结束
    @Schema(description = "是否已结束（已结束未结算的竞赛，结算时按重判后的结论计分）")
    private Boolean finished;

    // 该竞赛中会重判的提交数
    @Schema(description = "该竞赛中会重判的提交数")
    private Integer count;
}
