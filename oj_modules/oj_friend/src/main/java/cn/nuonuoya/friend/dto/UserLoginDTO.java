package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// C端用户短信验证码登录与注册请求参数DTO
@Getter
@Setter
@ToString
@Schema(description = "C端用户短信验证码登录与注册参数")
public class UserLoginDTO {

    // 手机号
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000001")
    private String phone;

    // 验证码
    @NotBlank(message = "验证码不能为空")
    @Schema(description = "6位短信验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String code;
}
