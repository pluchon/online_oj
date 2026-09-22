package cn.nuonuoya.api.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 测试用例输入生成请求
@Getter
@Setter
@ToString
public class AiCaseInputDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题目标题
    @NotBlank(message = "题目标题不能为空")
    @Size(max = 50, message = "题目标题不能超过50个字符")
    private String title;

    // 题目描述
    @NotBlank(message = "题目描述不能为空")
    @Size(max = 1000, message = "题目描述不能超过1000个字符")
    private String content;

    // 默认代码块（方法签名）
    @NotBlank(message = "默认代码块不能为空")
    @Size(max = 500, message = "默认代码块不能超过500个字符")
    private String defaultCode;

    // 主驱动函数（决定每组用例的标准输入格式）
    @NotBlank(message = "main函数不能为空")
    @Size(max = 5000, message = "main函数不能超过5000个字符")
    private String mainFunc;

    // 需要生成的用例数量
    @NotNull(message = "用例数量不能为空")
    @Min(value = 1, message = "用例数量至少为1")
    @Max(value = 10, message = "单次最多生成10组用例")
    private Integer count;

    // 已有用例的判题输入（用于去重，可为空）
    @Size(max = 50, message = "已有用例不能超过50组")
    private List<String> existingInputs;
}
