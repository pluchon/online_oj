package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 提交记录详情视图对象（公共字段见 SubmitBaseVO，另含代码、判题回显与首个未通过用例）
@Getter
@Setter
public class SubmitDetailVO extends SubmitBaseVO {

    // 用户代码
    @Schema(description = "用户代码")
    private String userCode;

    // 执行回显
    @Schema(description = "执行回显（编译错误、运行异常等）")
    private String exeMessage;

    // 逐用例状态
    @Schema(description = "逐用例状态（按用例顺序，1: 通过 0: 未通过 -: 未执行）")
    private String caseStates;

    // 首个未通过用例的判题输入
    @Schema(description = "首个未通过用例的判题输入（用例已删除时为空）")
    private String failCaseInput;

    // 首个未通过用例的预期输出
    @Schema(description = "首个未通过用例的预期输出（用例已删除时为空）")
    private String failCaseExpected;

    // 首个未通过用例的实际输出
    @Schema(description = "首个未通过用例的实际输出")
    private String failOutput;
}
