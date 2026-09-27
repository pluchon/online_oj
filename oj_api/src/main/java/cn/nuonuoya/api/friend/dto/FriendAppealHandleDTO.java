package cn.nuonuoya.api.friend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 申诉裁定请求
@Getter
@Setter
@ToString
public class FriendAppealHandleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 裁定结果（见 AppealStatusEnum：存疑、通过、不通过）
    private Integer status;

    // 裁定人（管理员ID，由调用方从登录上下文取得）
    private Long handlerId;
}
