package cn.nuonuoya.system.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目实体
@TableName("tb_question")
@Getter
@Setter
@ToString
public class TbQuestion extends BaseEntity {

    // 题目id，同样的是雪花算法
    @TableId(value = "QUESTION_ID", type = IdType.ASSIGN_ID)
    private Long questionId;

    // 题目标题
    private String title;

    // 题目难度（1:简单 2:中等 3:困难）
    private Integer difficulty;

    // 时间限制
    private Integer timeLimit;

    // 空间限制
    private Integer spaceLimit;

    // 题目内容
    private String content;

    // 题目用例

    // 默认代码块
    private String defaultCode;

    // main函数
    private String mainFunc;
}
