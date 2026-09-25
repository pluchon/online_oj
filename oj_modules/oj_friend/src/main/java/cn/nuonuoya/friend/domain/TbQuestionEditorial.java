package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目官方题解实体（C端只读）
@TableName("tb_question_editorial")
@Getter
@Setter
@ToString
public class TbQuestionEditorial extends BaseEntity {

    // 题解ID
    @TableId(value = "EDITORIAL_ID", type = IdType.ASSIGN_ID)
    private Long editorialId;

    // 题目ID
    private Long questionId;

    // 题解内容（Markdown）
    private String content;

    // 逻辑删除标识（0: 正常 1: 已删除）
    @TableLogic
    private Integer deleteState;
}
