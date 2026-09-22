package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// AI 帮建选出的题目
@Getter
@Setter
@Schema(description = "AI 帮建选出的题目")
public class ExamAiQuestionVO {

    // 题目ID
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "题目ID")
    private Long questionId;

    // 题目标题
    @Schema(description = "题目标题")
    private String title;

    // 难度
    @Schema(description = "难度（1:简单 2:中等 3:困难）")
    private Integer difficulty;

    // 难度描述
    @Schema(description = "难度描述")
    private String difficultyDesc;
}
