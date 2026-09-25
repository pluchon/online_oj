package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目标签关联实体（C端只读）
@TableName("tb_question_tag")
@Getter
@Setter
@ToString
public class TbQuestionTag extends BaseEntity {

    // 关联ID
    @TableId(value = "QUESTION_TAG_ID", type = IdType.ASSIGN_ID)
    private Long questionTagId;

    // 题目ID
    private Long questionId;

    // 标签ID
    private Long tagId;

    // 逻辑删除标识（0: 正常 1: 已删除）
    @TableLogic
    private Integer deleteState;
}
