package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

// 时间段提交统计（计数字段见 FriendSubmitStatBaseVO）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendPeriodStatVO extends FriendSubmitStatBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 活跃用户数（时间段内有提交的去重用户）
    private Integer activeUsers;
}
