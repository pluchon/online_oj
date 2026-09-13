package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 题目修改请求参数数据对象（继承新增入参，扩展必填题目ID）
@Getter
@Setter
@Schema(description = "题目修改请求参数")
public class QuestionEditDTO extends QuestionAddDTO {

    // 题目ID
    @Schema(description = "题目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "题目ID不能为空")
    private Long questionId;
}
