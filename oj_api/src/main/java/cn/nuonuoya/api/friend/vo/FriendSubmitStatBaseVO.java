package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 提交计数公共字段（时间段、每日趋势、难题统计共用）
@Getter
@Setter
@ToString
public class FriendSubmitStatBaseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 提交数（含评测中）
    private Integer submitCount = 0;

    // 已出结论的提交数（通过率的分母）
    private Integer judgedCount = 0;

    // 通过的提交数
    private Integer passCount = 0;
}
