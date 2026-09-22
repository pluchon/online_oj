package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 单组测试用例输入（不含预期输出，预期输出由标程实际运行得到）
@Getter
@Setter
@ToString
public class AiCaseInputItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 展示用输入，如 nums = [1,2], target = 3
    private String displayInput;

    // 判题用输入（一组用例的标准输入，不含首行用例数）
    private String judgeInput;

    // 设计意图，如"空数组边界"
    private String intent;
}
