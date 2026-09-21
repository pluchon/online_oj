package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 竞赛实体
@TableName("tb_exam")
@Getter
@Setter
@ToString
public class TbExam extends BaseEntity {

    // 竞赛id (主键，雪花算法)
    @TableId(value = "EXAM_ID", type = IdType.ASSIGN_ID)
    private Long examId;

    // 竞赛标题
    private String title;

    // 竞赛开始时间
    private LocalDateTime startTime;

    // 竞赛结束时间
    private LocalDateTime endTime;

    // 是否发布 0: 未发布 1: 已发布
    private Integer status;

    // 排名是否已结算（见 ExamRankSettledEnum）
    private Integer rankSettled;
}
