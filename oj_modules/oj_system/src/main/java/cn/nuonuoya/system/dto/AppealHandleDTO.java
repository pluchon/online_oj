package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 申诉裁定请求DTO
@Getter
@Setter
@Schema(description = "申诉裁定请求")
public class AppealHandleDTO {

    // 裁定结果（1: 存疑 2: 通过 3: 不通过）
    @NotNull(message = "请选择裁定结果")
    @Schema(description = "裁定结果 1:存疑 2:通过 3:不通过", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
