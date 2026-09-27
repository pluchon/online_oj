package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 难题榜题目视图对象（计数与通过率见 SubmitStatBaseVO）
@Getter
@Setter
public class OverviewQuestionVO extends SubmitStatBaseVO {

    // 题目ID
    @Schema(description = "题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    // 题目标题
    @Schema(description = "题目标题")
    private String title;

    // 题目难度描述
    @Schema(description = "题目难度描述")
    private String difficultyDesc;
}
