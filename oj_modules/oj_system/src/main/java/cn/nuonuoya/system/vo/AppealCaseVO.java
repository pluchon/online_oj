package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 申诉详情中的单个用例
@Getter
@Setter
public class AppealCaseVO {

    // 用例序号（从 1 开始）
    @Schema(description = "用例序号")
    private Integer index;

    // 用例ID
    @Schema(description = "用例ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long caseId;

    // 是否通过
    @Schema(description = "是否通过（未执行为空）")
    private Boolean pass;

    // 是否公开示例
    @Schema(description = "是否公开示例")
    private Boolean sample;

    // 判题输入
    @Schema(description = "判题输入（用例已被修改或删除时为空）")
    private String input;

    // 预期输出
    @Schema(description = "预期输出（用例已被修改或删除时为空）")
    private String expectedOutput;

    // 实际输出
    @Schema(description = "实际输出（通过的用例同预期输出，没有记录时为空）")
    private String actualOutput;

    // 用例是否已被修改或删除
    @Schema(description = "用例是否已被修改或删除")
    private Boolean missing;
}
