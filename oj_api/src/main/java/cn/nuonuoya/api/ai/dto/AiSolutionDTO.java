package cn.nuonuoya.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 解法示例生成请求
@Getter
@Setter
@ToString
public class AiSolutionDTO implements Serializable {

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
}
