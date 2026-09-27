package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// 难题分析的 AI 结论（数字由调用方统计，这里只有文字）
@Getter
@Setter
@ToString
public class AiHardAnalysisVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 整体薄弱点的结论与建议
    private String weakSummary;

    // 主要错误类型的结论与建议
    private String verdictSummary;

    // 每道可疑题的判断
    private List<AiHardSuspectVO> suspects = new ArrayList<>();
}
