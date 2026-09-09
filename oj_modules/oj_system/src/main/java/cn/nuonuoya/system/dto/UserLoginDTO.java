package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserLoginDTO{

    @Schema(description = "用户账号")
    private String userAccount;

    @Schema(description = "用户密码")
    private String password;
}
