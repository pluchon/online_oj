package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// C端个人中心资料修改入参DTO
@Getter
@Setter
public class UserProfileUpdateDTO {

    // 用户昵称
    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户昵称不能为空")
    @Size(min = 2, max = 32, message = "昵称长度必须在2到32个字符之间")
    private String nickName;

    // 用户头像URL
    @Schema(description = "用户头像URL")
    private String headImage;

    // 用户性别 0: 保密 1: 男 2: 女
    @Schema(description = "用户性别 0:保密 1:男 2:女", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别取值不合法")
    @Max(value = 2, message = "性别取值不合法")
    private Integer sex;

    // 邮箱
    @Schema(description = "邮箱")
    @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "邮箱格式不合法")
    private String email;

    // 微信号
    @Schema(description = "微信号")
    @Size(max = 50, message = "微信号不能超过50个字符")
    private String wechat;

    // 学校名称
    @Schema(description = "学校名称")
    @Size(max = 100, message = "学校名称不能超过100个字符")
    private String schoolName;

    // 专业名称
    @Schema(description = "专业名称")
    @Size(max = 100, message = "专业名称不能超过100个字符")
    private String majorName;

    // 个人介绍
    @Schema(description = "个人介绍")
    @Size(max = 200, message = "个人介绍不能超过200个字符")
    private String introduce;
}
