package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// C端个人中心用户视图对象
@Getter
@Setter
public class UserVO {

    // 用户ID（序列化为字符串防止精度丢失）
    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    // 用户昵称
    @Schema(description = "用户昵称")
    private String nickName;

    // 用户头像URL
    @Schema(description = "用户头像URL")
    private String headImage;

    // 用户性别 0: 保密 1: 男 2: 女
    @Schema(description = "用户性别 0:保密 1:男 2:女")
    private Integer sex;

    // 性别描述
    @Schema(description = "性别描述")
    private String sexDesc;

    // 脱敏手机号
    @Schema(description = "脱敏手机号")
    private String phone;

    // 邮箱
    @Schema(description = "邮箱")
    private String email;

    // 微信号
    @Schema(description = "微信号")
    private String wechat;

    // 学校名称
    @Schema(description = "学校名称")
    private String schoolName;

    // 专业名称
    @Schema(description = "专业名称")
    private String majorName;

    // 个人介绍
    @Schema(description = "个人介绍")
    private String introduce;

    // 帐号状态 0: 拉黑 1: 正常
    @Schema(description = "帐号状态 0:拉黑 1:正常")
    private Integer status;

    // 注册时间
    @Schema(description = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
