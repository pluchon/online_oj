package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目实体类
@TableName("tb_question")
@Getter
@Setter
@ToString
public class TbQuestion extends BaseEntity {

    // 题目主键ID
    @TableId(value = "QUESTION_ID", type = IdType.ASSIGN_ID)
    private Long questionId;

    // 题目标题
    private String title;

    // 题目难度（1:简单 2:中等 3:困难）
    private Integer difficulty;

    // 时间限制（毫秒）
    private Integer timeLimit;

    // 空间限制（MB）
    private Integer spaceLimit;

    // 题目内容描述
    private String content;

    // 题目测试用例
    private String questionCase;

    // 默认代码模板
    private String defaultCode;

    // 评测主函数
    private String mainFunc;
}
