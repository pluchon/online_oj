package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 用户信息详情
@Getter
@Setter
public class SysUserVO {

    // 用户ID
    @Schema(description = "用户ID")
    private Long userId;

    // 用户账号
    @Schema(description = "用户账号")
    private String userAccount;

    // 用户昵称
    @Schema(description = "用户昵称")
    private String nickName;
}
