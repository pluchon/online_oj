package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 题解草稿（Markdown，由管理员确认后随题目保存）
@Getter
@Setter
@ToString
public class AiEditorialVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题解内容（Markdown：思路、复杂度、代码）
    private String content;
}
