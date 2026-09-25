package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// 题目视图对象
@Getter
@Setter
@Schema(description = "题目视图对象")
public class QuestionVO {

    // 题目ID（转字符串防止前端精度丢失）
    @Schema(description = "题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    // 题目标题
    @Schema(description = "题目标题")
    private String title;

    // 题目难度（1:简单 2:中等 3:困难）
    @Schema(description = "题目难度（1:简单 2:中等 3:困难）")
    private Integer difficulty;

    // 题目难度描述（简单、中等、困难）
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

    // 默认代码模板
    @Schema(description = "默认代码模板")
    private String defaultCode;

    // 公开示例（仅题目详情返回，隐藏用例不对外）
    @Schema(description = "公开示例")
    private List<QuestionCaseVO> sampleCases;

    // 创建时间
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 学员做题状态（0:未尝试 1:已攻克 2:尝试中）
    @Schema(description = "学员做题状态（0:未尝试 1:已攻克 2:尝试中）")
    private Integer userStatus;

    // 题目标签列表
    @Schema(description = "题目标签列表")
    private List<QuestionTagVO> tags;

    // 是否为语义推荐结果（关键词无匹配时由语义检索补充）
    private Boolean semantic;
}
