package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 用户列表项视图对象
@Getter
@Setter
@ToString
@Schema(description = "用户列表项视图对象")
public class UserVO {

    // 用户ID（雪花算法，序列化为字符串防止前端精度丢失）
    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    // 用户昵称
    @Schema(description = "用户昵称")
    private String nickName;

    // 用户头像
    @Schema(description = "用户头像")
    private String headImage;

    // 用户性别数值（0: 保密 1: 男 2: 女）
    @Schema(description = "用户性别数值")
    private Integer sex;

    // 用户性别描述（保密 / 男 / 女）
    @Schema(description = "用户性别描述")
    private String sexDesc;

    // 手机号
    @Schema(description = "手机号")
    private String phone;

    // 邮箱
    @Schema(description = "邮箱")
    private String email;

    // 微信号
    @Schema(description = "微信号")
    private String wechat;

    // QQ号
    @Schema(description = "QQ号")
    private String qq;

    // 学校
    @Schema(description = "学校")
    private String schoolName;

    // 专业
    @Schema(description = "专业")
    private String majorName;

    // 个人介绍
    @Schema(description = "个人介绍")
    private String introduce;

    // 用户状态数值（0: 拉黑 1: 正常）
    @Schema(description = "用户状态数值")
    private Integer status;

    // 用户状态描述（拉黑 / 正常）
    @Schema(description = "用户状态描述")
    private String statusDesc;

    // 创建时间
    @Schema(description = "注册创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
