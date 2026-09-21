package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 管理员编辑C端用户资料请求参数（校验规则与C端个人资料保持一致）
@Getter
@Setter
@Schema(description = "编辑用户资料参数")
public class UserEditDTO {

    // 用户ID（由路径参数填充）
    @Schema(hidden = true)
    private Long userId;

    // 用户昵称
    @NotBlank(message = "用户昵称不能为空")
    @Size(min = 2, max = 32, message = "昵称长度必须在2到32个字符之间")
    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickName;

    // 用户性别 0: 保密 1: 男 2: 女
    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别取值不合法")
    @Max(value = 2, message = "性别取值不合法")
    @Schema(description = "用户性别 0:保密 1:男 2:女", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sex;

    // 手机号（C端登录凭据，必填且全局唯一）
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不合法")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    // 邮箱
    @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "邮箱格式不合法")
    @Schema(description = "邮箱")
    private String email;

    // 微信号
    @Size(max = 50, message = "微信号不能超过50个字符")
    @Schema(description = "微信号")
    private String wechat;

    // 学校名称
    @Size(max = 100, message = "学校名称不能超过100个字符")
    @Schema(description = "学校名称")
    private String schoolName;

    // 专业名称
    @Size(max = 100, message = "专业名称不能超过100个字符")
    @Schema(description = "专业名称")
    private String majorName;

    // 个人介绍
    @Size(max = 200, message = "个人介绍不能超过200个字符")
    @Schema(description = "个人介绍")
    private String introduce;
}
