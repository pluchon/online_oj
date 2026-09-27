package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 一份未通过提交的代码样本（供 AI 判断用例是否有误）
@Getter
@Setter
@ToString
public class FriendFailedSampleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 学员代码
    private String userCode;

    // 首个未通过用例的实际输出（未记录时为空）
    private String failOutput;
}
