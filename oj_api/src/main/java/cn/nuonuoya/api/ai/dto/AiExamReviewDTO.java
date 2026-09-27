package cn.nuonuoya.api.ai.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// 赛后复盘请求：竞赛成绩与逐题情况
@Getter
@Setter
@ToString
public class AiExamReviewDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 竞赛名称
    private String examTitle;

    // 得分
    private Integer score;

    // 排名
    private Integer examRank;

    // 参赛人数（报名人数）
    private Integer participantCount;

    // 逐题情况（按竞赛题目顺序）
    @Valid
    private List<AiExamReviewQuestionDTO> questions = new ArrayList<>();
}
