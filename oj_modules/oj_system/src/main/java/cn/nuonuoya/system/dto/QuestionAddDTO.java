package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 题目新增请求参数数据对象
@Getter
@Setter
@Schema(description = "题目新增请求参数")
public class QuestionAddDTO {

    // 题目标题
    @Schema(description = "题目标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "题目标题不能为空")
    @Size(max = 50, message = "题目标题长度不能超过50个字符")
    private String title;

    // 题目难度（1:简单 2:中等 3:困难）
    @Schema(description = "题目难度（1:简单 2:中等 3:困难）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "题目难度不能为空")
    @Min(value = 1, message = "题目难度值不合法")
    @Max(value = 3, message = "题目难度值不合法")
    private Integer difficulty;

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

    // 题目内容描述
    @Schema(description = "题目内容描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "题目内容描述不能为空")
    @Size(max = 1000, message = "题目内容描述长度不能超过1000个字符")
    private String content;

    // 题目用例
    @Schema(description = "题目用例", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "题目用例不能为空")
    @Size(max = 1000, message = "题目用例长度不能超过1000个字符")
    private String questionCase;

    // 默认代码块
    @Schema(description = "默认代码块", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "默认代码块不能为空")
    @Size(max = 500, message = "默认代码块长度不能超过500个字符")
    private String defaultCode;

    // main函数
    @Schema(description = "main函数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "main函数不能为空")
    @Size(max = 500, message = "main函数长度不能超过500个字符")
    private String mainFunc;
}
