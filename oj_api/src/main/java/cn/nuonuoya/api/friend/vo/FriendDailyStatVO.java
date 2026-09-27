package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.time.LocalDate;

// 单日提交统计（计数字段见 FriendSubmitStatBaseVO）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendDailyStatVO extends FriendSubmitStatBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 日期
    private LocalDate date;
}
