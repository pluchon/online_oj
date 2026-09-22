package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// AI 生成测试用例的预览结果
@Getter
@Setter
@Schema(description = "AI 生成测试用例预览")
public class QuestionAiCaseVO {

    // 可用的用例
    @Schema(description = "可用的用例")
    private List<QuestionAiCaseItemVO> cases;

    // 模型给出的输入组数
    @Schema(description = "模型给出的输入组数")
    private Integer generatedCount;

    // 因标程运行失败或输出超长而丢弃的组数
    @Schema(description = "丢弃的组数")
    private Integer droppedCount;
}
