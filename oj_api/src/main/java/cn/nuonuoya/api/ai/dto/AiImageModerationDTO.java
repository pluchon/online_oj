package cn.nuonuoya.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 图片审核请求
@Getter
@Setter
@ToString(exclude = "data")
public class AiImageModerationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 图片 MIME 类型，如 image/png
    @NotBlank(message = "图片类型不能为空")
    private String mimeType;

    // 图片内容（JSON 中为 Base64，最大 2MB）
    @NotNull(message = "图片内容不能为空")
    @Size(max = 2 * 1024 * 1024, message = "图片不能超过2MB")
    private byte[] data;
}
