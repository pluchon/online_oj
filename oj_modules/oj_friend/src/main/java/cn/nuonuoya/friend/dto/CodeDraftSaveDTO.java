package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 保存代码草稿请求参数
@Getter
@Setter
@Schema(description = "保存代码草稿请求参数")
public class CodeDraftSaveDTO {

    // 代码内容
    @Schema(description = "代码内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "代码不能为空")
    @Size(max = 10000, message = "代码不能超过10000个字符")
    private String code;
}
