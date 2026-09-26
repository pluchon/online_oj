package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 重判涉及的竞赛
@Getter
@Setter
@ToString
public class FriendRejudgeExamVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 竞赛ID
    private Long examId;

    // 竞赛标题
    private String title;

    // 是否已结束（已结束未结算的竞赛，结算时按重判后的结论计分）
    private Boolean finished;

    // 该竞赛中会重判的提交数
    private Integer count;
}
