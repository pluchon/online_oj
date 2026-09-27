package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// 题目列表项视图对象
@Getter
@Setter
public class QuestionVO {

    // 题目ID（转为字符串避免前端JS大数精度丢失）
    @Schema(description = "题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    // 题目标题
    @Schema(description = "题目标题")
    private String title;

    // 题目难度（1:简单 2:中等 3:困难）
    @Schema(description = "题目难度（1:简单 2:中等 3:困难）")
    private Integer difficulty;

    // 题目难度描述（简单/中等/困难）
    @Schema(description = "题目难度描述")
    private String difficultyDesc;

    // 题目用途（1:刷题 2:竞赛）
    @Schema(description = "题目用途（1:刷题 2:竞赛）")
    private Integer purpose;

    // 题目用途描述
    @Schema(description = "题目用途描述")
    private String purposeDesc;

    // 是否已公开（用到它的竞赛里至少有一场已结束，不能再用于新竞赛）
    @Schema(description = "是否已公开")
    private Boolean published;

    // 创建人昵称（通过联查用户表获得）
    @Schema(description = "创建人")
    private String creatorName;

    // 题目标签
    @Schema(description = "题目标签")
    private List<QuestionTagVO> tags;

    // 申诉成立、待修题的数量
    @Schema(description = "申诉成立、待修题的数量（裁定为通过的申诉晚于用例最后修改；0 表示没有）")
    private Integer upheldAppealCount;

    // 创建时间
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
