package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 题目修改请求参数数据对象（公共字段见 QuestionBaseDTO，另带题目ID）
@Getter
@Setter
@Schema(description = "题目修改请求参数")
public class QuestionEditDTO extends QuestionBaseDTO {

    // 题目ID
    @Schema(description = "题目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long questionId;
}
