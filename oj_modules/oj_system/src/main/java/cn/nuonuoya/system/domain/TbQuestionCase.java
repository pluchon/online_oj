package cn.nuonuoya.system.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目测试用例实体
@TableName("tb_question_case")
@Getter
@Setter
@ToString
public class TbQuestionCase extends BaseEntity {

    // 用例主键ID（自增）
    @TableId(value = "CASE_ID", type = IdType.AUTO)
    private Long caseId;

    // 题目ID
    private Long questionId;

    // 展示用输入（题面显示）
    private String displayInput;

    // 展示用输出（题面显示）
    private String displayOutput;

    // 判题用输入（按 main 函数约定喂入标准输入）
    private String judgeInput;

    // 判题用预期输出（单行）
    private String judgeOutput;

    // 用例类型（见 QuestionCaseType）
    private Integer isSample;

    // 排序（升序）
    private Integer sortOrder;

    // 逻辑删除标识（0: 正常 1: 已删除）
    @TableLogic
    private Integer deleteState;
}
