package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// AI 生成的单组测试用例（预期输出来自标程运行）
@Getter
@Setter
@Schema(description = "AI 生成的测试用例")
public class QuestionAiCaseItemVO {

    // 展示用输入
    @Schema(description = "展示用输入")
    private String displayInput;

    // 展示用输出
    @Schema(description = "展示用输出")
    private String displayOutput;

    // 判题用输入
    @Schema(description = "判题用输入")
    private String judgeInput;

    // 判题用预期输出（标程运行结果）
    @Schema(description = "判题用预期输出")
    private String judgeOutput;

    // 是否公开示例（默认隐藏用例）
    @Schema(description = "1: 公开示例 0: 隐藏用例")
    private Integer isSample;

    // 设计意图
    @Schema(description = "设计意图")
    private String intent;
}
