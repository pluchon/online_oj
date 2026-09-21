package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// 题目详情视图对象
@Getter
@Setter
@Schema(description = "题目详情视图对象")
public class QuestionDetailVO {

    // 题目ID（序列化为字符串防止JS前端丢失精度）
    @Schema(description = "题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    // 题目标题
    @Schema(description = "题目标题")
    private String title;

    // 题目难度（1:简单 2:中等 3:困难）
    @Schema(description = "题目难度（1:简单 2:中等 3:困难）")
    private Integer difficulty;

    // 题目难度描述文案
    @Schema(description = "题目难度描述")
    private String difficultyDesc;

    // 时间限制（毫秒）
    @Schema(description = "时间限制（毫秒）")
    private Integer timeLimit;

    // 空间限制（MB）
    @Schema(description = "空间限制（MB）")
    private Integer spaceLimit;

    // 题目内容描述
    @Schema(description = "题目内容描述")
    private String content;

    // 测试用例（含隐藏用例）
    @Schema(description = "测试用例")
    private List<QuestionCaseVO> cases;

    // 默认代码块
    @Schema(description = "默认代码块")
    private String defaultCode;

    // main函数
    @Schema(description = "main函数")
    private String mainFunc;

    // 创建人昵称
    @Schema(description = "创建人")
    private String creatorName;

    // 创建时间
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
