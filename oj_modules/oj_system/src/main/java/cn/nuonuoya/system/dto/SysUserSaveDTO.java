package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 新增管理员请求参数
@Getter
@Setter
@ToString
public class SysUserSaveDTO {

    // 用户账号
    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userAccount;

    // 用户密码
    @Schema(description = "用户密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    // 用户昵称
    @Schema(description = "用户昵称")
    private String nickName;
}
