package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 解法示例（与用户提交格式相同：实现给定方法，可含辅助方法，不含类与 import）
@Getter
@Setter
@ToString
public class AiSolutionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 解法代码
    private String code;
}
