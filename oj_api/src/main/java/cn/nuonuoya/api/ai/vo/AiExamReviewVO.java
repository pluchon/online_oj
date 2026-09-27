package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// 赛后复盘的 AI 结论（数字由调用方统计，这里只有文字）
@Getter
@Setter
@ToString
public class AiExamReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 整体总结：这场的主要问题与接下来该补的方向
    private String summary;

    // 逐题点评（没有提交的题不点评）
    private List<AiExamReviewCommentVO> questions = new ArrayList<>();
}
