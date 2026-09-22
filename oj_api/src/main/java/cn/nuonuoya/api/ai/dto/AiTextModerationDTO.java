package cn.nuonuoya.api.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 文本审核请求
@Getter
@Setter
@ToString
public class AiTextModerationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 待审核的文本（如昵称、个人简介）
    @NotEmpty(message = "文本不能为空")
    @Size(max = 10, message = "单次最多审核10条文本")
    private List<@Size(max = 1000, message = "单条文本不能超过1000个字符") String> texts;
}
