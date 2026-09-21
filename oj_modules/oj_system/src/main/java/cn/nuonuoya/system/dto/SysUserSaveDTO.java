package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 新增管理员请求参数
@Getter
@Setter
public class SysUserSaveDTO {

    // 用户账号
    @NotBlank(message = "账号不能为空")
    @Size(max = 32, message = "账号长度不能超过32位")
    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userAccount;

    // 用户密码
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为6到20位")
    @Schema(description = "用户密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    // 用户昵称
    @Size(max = 32, message = "昵称长度不能超过32位")
    @Schema(description = "用户昵称")
    private String nickName;
}
