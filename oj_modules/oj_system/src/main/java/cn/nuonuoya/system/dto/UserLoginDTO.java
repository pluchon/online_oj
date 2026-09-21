package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// 管理员登录请求参数
@Getter
@Setter
public class UserLoginDTO {

    // 用户账号
    @NotBlank(message = "账号不能为空")
    @Schema(description = "用户账号")
    private String userAccount;

    // 用户密码
    @NotBlank(message = "密码不能为空")
    @Schema(description = "用户密码")
    private String password;
}
