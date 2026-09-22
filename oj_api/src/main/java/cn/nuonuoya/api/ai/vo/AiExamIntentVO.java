package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 竞赛需求理解结果
@Getter
@Setter
@ToString
public class AiExamIntentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 竞赛名称
    private String title;

    // 主题关键词（空格分隔，用于检索题目）
    private String topics;

    // 描述中明确给出的题目数量，没有时为空
    private Integer questionCount;
}
