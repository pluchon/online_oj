package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 赛后复盘实体（一人一场一条，提交结果变化后重新生成时原地覆盖）
@TableName("tb_exam_review")
@Getter
@Setter
@ToString
public class TbExamReview extends BaseEntity {

    // 复盘ID（雪花算法）
    @TableId(value = "REVIEW_ID", type = IdType.ASSIGN_ID)
    private Long reviewId;

    // 学员ID
    private Long userId;

    // 竞赛ID
    private Long examId;

    // 复盘内容（ExamReviewVO 的 JSON）
    private String content;

    // 生成时本人这场提交结果的摘要
    private String sourceDigest;

    // 学员手动重新生成的次数
    private Integer regenerateCount;

    // 逻辑删除标识（0: 正常 1: 已删除）
    @TableLogic
    private Integer deleteState;
}
