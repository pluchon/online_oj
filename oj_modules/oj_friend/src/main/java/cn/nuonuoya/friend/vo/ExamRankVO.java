package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 竞赛排名榜单选手视图对象
@Getter
@Setter
public class ExamRankVO {

    // 排名名次（从1开始）
    @Schema(description = "排名名次")
    private Integer examRank;

    // 用户ID（防前端精度丢失转为字符串）
    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    // 用户昵称
    @Schema(description = "用户昵称")
    private String nickName;

    // 用户头像URL
    @Schema(description = "用户头像URL")
    private String headImage;

    // 竞赛总得分
    @Schema(description = "竞赛总得分")
    private Integer score;

    // 通过题目数量
    @Schema(description = "通过题目数")
    private Integer acceptCount;

    // 总提交次数
    @Schema(description = "总提交次数")
    private Integer submitCount;

    // 最后一次有效得分提交时间
    @Schema(description = "最后提交时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSubmitTime;

    // 是否为当前登录用户
    @Schema(description = "是否当前登录用户")
    private Boolean isCurrentUser;
}
