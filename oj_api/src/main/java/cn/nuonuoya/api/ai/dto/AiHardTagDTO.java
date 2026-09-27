package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 难题分析中一个标签的通过率
@Getter
@Setter
@ToString
public class AiHardTagDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 标签名称
    private String tagName;

    // 该标签下参与分析的题数
    private Integer questionCount;

    // 已出结论的提交数
    private Integer judgedCount;

    // 通过率（百分比，一位小数）
    private Double passRate;
}
