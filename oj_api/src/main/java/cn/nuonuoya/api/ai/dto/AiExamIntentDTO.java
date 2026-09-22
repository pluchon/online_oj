package cn.nuonuoya.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 竞赛需求理解请求
@Getter
@Setter
@ToString
public class AiExamIntentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 管理员的一句话描述
    @NotBlank(message = "描述不能为空")
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
}
