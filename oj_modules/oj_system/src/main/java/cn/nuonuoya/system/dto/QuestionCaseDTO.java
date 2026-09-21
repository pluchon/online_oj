package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 题目测试用例请求参数（展示格式用于题面，判题格式用于喂入程序）
@Getter
@Setter
@Schema(description = "题目测试用例")
public class QuestionCaseDTO {

    // 展示用输入
    @NotBlank(message = "用例展示输入不能为空")
    @Size(max = 2000, message = "用例展示输入不能超过2000个字符")
    @Schema(description = "展示用输入，如 s = \"()\"")
    private String displayInput;

    // 展示用输出
    @NotBlank(message = "用例展示输出不能为空")
    @Size(max = 2000, message = "用例展示输出不能超过2000个字符")
    @Schema(description = "展示用输出")
    private String displayOutput;

    // 判题用输入
    @NotBlank(message = "用例判题输入不能为空")
    @Size(max = 10000, message = "用例判题输入不能超过10000个字符")
    @Schema(description = "判题用输入，按 main 函数约定逐行给出参数")
    private String judgeInput;

    // 判题用预期输出
    @NotBlank(message = "用例判题输出不能为空")
    @Size(max = 2000, message = "用例判题输出不能超过2000个字符")
    @Schema(description = "判题用预期输出（单行）")
    private String judgeOutput;

    // 是否公开示例（1: 公开示例 0: 隐藏用例）
    @NotNull(message = "用例类型不能为空")
    @Min(value = 0, message = "用例类型不合法")
    @Max(value = 1, message = "用例类型不合法")
    @Schema(description = "1: 公开示例 0: 隐藏用例")
    private Integer isSample;
}
