package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 修改用户状态DTO
@Getter
@Setter
@ToString
@Schema(description = "修改用户状态参数")
public class UserStatusDTO {

    // 用户ID
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    // 用户状态（0: 拉黑 1: 正常）
    @NotNull(message = "状态值不能为空")
    @Schema(description = "用户状态（0: 拉黑 1: 正常）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
