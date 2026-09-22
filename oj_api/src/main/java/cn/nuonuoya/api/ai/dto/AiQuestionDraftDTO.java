package cn.nuonuoya.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 题面草稿生成请求
@Getter
@Setter
@ToString
public class AiQuestionDraftDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 管理员输入的一句话题目描述
    @NotBlank(message = "题目描述不能为空")
    @Size(max = 500, message = "题目描述不能超过500个字符")
    private String description;
}
