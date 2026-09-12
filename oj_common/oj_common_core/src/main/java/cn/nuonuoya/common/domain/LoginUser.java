package cn.nuonuoya.common.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

// 登录用户信息载体
@Setter
@Getter
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 身份标识 1代表普通用户 2代表管理员用户
    private Integer identity;

    // 用户昵称
    private String nickName;
}
