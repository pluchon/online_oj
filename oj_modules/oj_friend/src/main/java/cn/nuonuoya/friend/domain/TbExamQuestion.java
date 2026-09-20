package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 竞赛题目关系实体
@TableName("tb_exam_question")
@Getter
@Setter
@ToString
public class TbExamQuestion extends BaseEntity {

    // 竞赛题目关系id (主键，雪花算法)
    @TableId(value = "EXAM_QUESTION_ID", type = IdType.ASSIGN_ID)
    private Long examQuestionId;

    // 题目id
    private Long questionId;

    // 竞赛id
    private Long examId;

    // 题目顺序
    private Integer questionOrder;
}
