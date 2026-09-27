package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// AI 对一道题的点评
@Getter
@Setter
@ToString
public class AiExamReviewCommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题目序号（与请求中的 index 对应）
    private Integer index;

    // 一句话点评：卡在哪、往哪个方向想，或前面错在哪
    private String comment;
}
