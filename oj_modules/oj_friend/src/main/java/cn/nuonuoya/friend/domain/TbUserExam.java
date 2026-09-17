package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 用户竞赛关联实体（记录报名、得分与排名）
@TableName("tb_user_exam")
@Getter
@Setter
@ToString
public class TbUserExam extends BaseEntity {

    // 用户竞赛关系id (主键，雪花算法)
    @TableId(value = "USER_EXAM_ID", type = IdType.ASSIGN_ID)
    private Long userExamId;

    // 用户id
    private Long userId;

    // 竞赛id
    private Long examId;

    // 竞赛得分
    private Integer score;

    // 竞赛排名
    private Integer examRank;
}
