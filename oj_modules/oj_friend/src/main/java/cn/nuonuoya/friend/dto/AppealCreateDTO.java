package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 正式申诉请求DTO
@Getter
@Setter
@Schema(description = "正式申诉请求DTO")
public class AppealCreateDTO {

    // 被申诉的提交ID
    @NotNull(message = "请选择要申诉的提交")
    @Schema(description = "被申诉的提交ID")
    private Long submitId;

    // 申诉理由
    @NotBlank(message = "请填写申诉理由")
    @Size(max = 100, message = "申诉理由不能超过100个字符")
    @Schema(description = "申诉理由")
    private String reason;
}
