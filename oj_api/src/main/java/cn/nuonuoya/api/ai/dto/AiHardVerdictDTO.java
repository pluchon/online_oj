package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 难题分析中一种判题结论的占比
@Getter
@Setter
@ToString
public class AiHardVerdictDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 判题结论描述（如答案错误、运行超时）
    private String verdict;

    // 未通过提交数
    private Integer failCount;

    // 占全部未通过提交的百分比（一位小数）
    private Double share;
}
