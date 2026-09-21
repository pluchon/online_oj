package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 运行示例用例请求DTO
@Getter
@Setter
@Schema(description = "运行示例用例请求DTO")
public class QuestionRunDTO {

    // 题目ID
    @NotNull(message = "题目ID不能为空")
    @Schema(description = "题目ID")
    private Long questionId;

    // 用户编写的代码
    @NotBlank(message = "代码内容不能为空")
    @Schema(description = "用户编写的代码")
    private String userCode;
}
