package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 候选题目
@Getter
@Setter
@ToString
public class FriendQuestionCandidateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题目ID
    private Long questionId;

    // 题目标题
    private String title;

    // 难度（1:简单 2:中等 3:困难）
    private Integer difficulty;

    // 描述摘要
    private String summary;

    // 通过率（0~1，提交数不足时为空）
    private Double passRate;
}
