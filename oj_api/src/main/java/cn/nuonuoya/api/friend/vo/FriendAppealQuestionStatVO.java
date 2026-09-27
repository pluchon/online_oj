package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

// 单题成立申诉统计
@Getter
@Setter
@ToString
public class FriendAppealQuestionStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题目ID
    private Long questionId;

    // 裁定为通过的申诉数
    private Integer upheldCount;

    // 最近一次裁定为通过的时间
    private LocalDateTime latestHandleTime;
}
