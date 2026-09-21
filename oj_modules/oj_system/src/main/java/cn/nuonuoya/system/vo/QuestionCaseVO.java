package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 题目测试用例视图对象（管理端可见全部用例）
@Getter
@Setter
@Schema(description = "题目测试用例")
public class QuestionCaseVO {

    // 展示用输入
    @Schema(description = "展示用输入")
    private String displayInput;

    // 展示用输出
    @Schema(description = "展示用输出")
    private String displayOutput;

    // 判题用输入
    @Schema(description = "判题用输入")
    private String judgeInput;

    // 判题用预期输出
    @Schema(description = "判题用预期输出")
    private String judgeOutput;

    // 是否公开示例（1: 公开示例 0: 隐藏用例）
    @Schema(description = "1: 公开示例 0: 隐藏用例")
    private Integer isSample;
}
