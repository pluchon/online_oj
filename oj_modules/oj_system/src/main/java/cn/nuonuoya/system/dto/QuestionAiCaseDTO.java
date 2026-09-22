package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// AI 生成测试用例请求参数（标程只用于运行得到预期输出，不入库）
@Getter
@Setter
@Schema(description = "AI 生成测试用例请求参数")
public class QuestionAiCaseDTO {

    // 题目标题
    @Schema(description = "题目标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "题目标题不能为空")
    @Size(max = 50, message = "题目标题长度不能超过50个字符")
    private String title;

    // 题目描述
    @Schema(description = "题目描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "题目内容描述不能为空")
    @Size(max = 1000, message = "题目内容描述长度不能超过1000个字符")
    private String content;

    // 默认代码块
    @Schema(description = "默认代码块", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "默认代码块不能为空")
    @Size(max = 500, message = "默认代码块长度不能超过500个字符")
    private String defaultCode;

    // main函数
    @Schema(description = "main函数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "main函数不能为空")
    @Size(max = 5000, message = "main函数长度不能超过5000个字符")
    private String mainFunc;

    // 标程（与用户提交格式相同的方法实现；为空时由 AI 先生成解法再作为标程）
    @Schema(description = "标程，与用户提交格式相同；为空时由 AI 生成")
    @Size(max = 10000, message = "标程长度不能超过10000个字符")
    private String standardCode;

    // 时间限制（毫秒）
    @Schema(description = "时间限制（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "时间限制不能为空")
    @Min(value = 1, message = "时间限制必须大于0")
    private Integer timeLimit;

    // 空间限制（MB）
    @Schema(description = "空间限制（MB）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "空间限制不能为空")
    @Min(value = 1, message = "空间限制必须大于0")
    private Integer spaceLimit;

    // 生成数量（为空时由 AI 按题目复杂度在 2~5 组之间决定）
    @Schema(description = "生成数量（1~10），为空时由 AI 决定")
    @Min(value = 1, message = "生成数量至少为1")
    @Max(value = 10, message = "单次最多生成10组用例")
    private Integer count;

    // 表单中已有用例的判题输入（用于去重）
    @Schema(description = "已有用例的判题输入")
    @Size(max = 50, message = "已有用例不能超过50组")
    private List<String> existingInputs;
}
