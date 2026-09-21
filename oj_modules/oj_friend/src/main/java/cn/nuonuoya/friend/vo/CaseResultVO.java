package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 单个用例执行结果视图对象
@Getter
@Setter
@Schema(description = "单个用例执行结果视图对象")
public class CaseResultVO {

    // 展示用输入
    @Schema(description = "输入")
    private String input;

    // 展示用预期输出
    @Schema(description = "预期输出")
    private String expectedOutput;

    // 实际输出（程序未执行到该用例时为空）
    @Schema(description = "实际输出")
    private String actualOutput;

    // 是否通过
    @Schema(description = "是否通过")
    private Boolean pass;
}
