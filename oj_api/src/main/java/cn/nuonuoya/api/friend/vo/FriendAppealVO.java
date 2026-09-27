package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

// 申诉列表项（字段见 FriendAppealBaseVO）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendAppealVO extends FriendAppealBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;
}
