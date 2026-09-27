package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

// 难题分析中按标签汇总的提交统计（计数字段见 FriendSubmitStatBaseVO，只含达到门槛的题）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendTagStatVO extends FriendSubmitStatBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 标签名称
    private String tagName;

    // 该标签下达到门槛的题数
    private Integer questionCount = 0;
}
