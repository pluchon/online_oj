package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 按题重判的影响范围
@Getter
@Setter
@ToString
public class FriendRejudgePreviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 本次会重判的提交数（练习 + 未结算竞赛）
    private Integer rejudgeCount;

    // 其中练习提交数
    private Integer practiceCount;

    // 涉及的未结算竞赛及各自的提交数
    private List<FriendRejudgeExamVO> exams;

    // 跳过的已结算竞赛提交数
    private Integer settledCount;

    // 跳过的评测中提交数
    private Integer judgingCount;
}
