package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

// 难题分析中的一道可疑题（题面见 AiQuestionBaseDTO；失败最集中的用例与抽查的失败代码）
@Getter
@Setter
@ToString(callSuper = true)
public class AiHardSuspectDTO extends AiQuestionBaseDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 序号（从 1 开始，AI 按它返回判断）
    private Integer index;

    // 被列为可疑的原因（如「78% 的失败在用例 4」「2 条申诉成立」）
    private String reason;

    // 失败最集中的用例序号（从 1 开始，没有时为空）
    private Integer caseIndex;

    // 该用例是否公开示例
    private Boolean caseSample;

    // 该用例的判题输入
    private String caseInput;

    // 该用例的预期输出
    private String caseOutput;

    // 抽查的未通过代码
    private List<AiHardSampleDTO> samples = new ArrayList<>();
}
