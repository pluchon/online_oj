package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

// 提交记录列表项（字段见 FriendSubmitBaseVO）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendSubmitVO extends FriendSubmitBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;
}
