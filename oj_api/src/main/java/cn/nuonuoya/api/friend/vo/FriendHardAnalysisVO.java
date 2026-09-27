package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// 难题分析统计：达到门槛的题（已出结论的提交满 5 条）的单题、按标签与按判题结论三组数字
@Getter
@Setter
@ToString
public class FriendHardAnalysisVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 达到门槛的题，按通过率升序
    private List<FriendHardQuestionStatVO> questions = new ArrayList<>();

    // 按标签汇总，按通过率升序
    private List<FriendTagStatVO> tags = new ArrayList<>();

    // 未通过提交按判题结论分布，按数量降序
    private List<FriendVerdictStatVO> verdicts = new ArrayList<>();
}
