package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 题面草稿（字段与题目表单一一对应，由管理员确认后保存）
@Getter
@Setter
@ToString
public class AiQuestionDraftVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题目标题
    private String title;

    // 题目难度（1:简单 2:中等 3:困难）
    private Integer difficulty;

    // 时间限制（毫秒）
    private Integer timeLimit;

    // 空间限制（MB）
    private Integer spaceLimit;

    // 题目描述（Markdown，不含示例）
    private String content;

    // 默认代码块（方法签名）
    private String defaultCode;

    // 主驱动函数（按标准输入约定读取用例并逐行输出结果）
    private String mainFunc;

    // 建议标签名称（只含候选标签中的名称）
    private List<String> tags;
}
