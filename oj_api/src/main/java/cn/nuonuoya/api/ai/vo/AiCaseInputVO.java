package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 测试用例输入生成结果
@Getter
@Setter
@ToString
public class AiCaseInputVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 生成的用例输入
    private List<AiCaseInputItemVO> cases;
}
