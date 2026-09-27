package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

// 单题提交统计（计数字段见 FriendSubmitStatBaseVO）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendQuestionStatVO extends FriendSubmitStatBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题目ID
    private Long questionId;
}
