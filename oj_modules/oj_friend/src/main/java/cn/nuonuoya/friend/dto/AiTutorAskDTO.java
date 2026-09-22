package cn.nuonuoya.friend.dto;

import cn.nuonuoya.api.ai.enums.AiTutorActionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI 辅导提问请求参数
@Getter
@Setter
@Schema(description = "AI 辅导提问请求参数")
public class AiTutorAskDTO {

    // 提问类型（0:自由提问 1:思路 2:分析最近一次提交 3:解释编译错误 4:点评代码）
    @Schema(description = "提问类型（0:自由提问 1:思路 2:分析最近一次提交 3:解释编译错误 4:点评代码）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "提问类型不能为空")
    @Min(value = AiTutorActionEnum.MIN_CODE, message = "提问类型不合法")
    @Max(value = AiTutorActionEnum.MAX_CODE, message = "提问类型不合法")
    private Integer action;

    // 提问内容（自由提问时必填）
    @Schema(description = "提问内容，自由提问时必填")
    @Size(max = 500, message = "提问不能超过500个字符")
    private String content;

    // 编辑器中的当前代码
    @Schema(description = "编辑器中的当前代码")
    @Size(max = 10000, message = "代码不能超过10000个字符")
    private String userCode;
}
