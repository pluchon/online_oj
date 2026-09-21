package cn.nuonuoya.api.judge.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 判题服务执行请求数据传输对象
@Getter
@Setter
@ToString
public class JudgeRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 提交记录ID
    private Long submitId;

    // 题目ID
    private Long questionId;

    // 提交用户ID（用于全链路审计与日志排障）
    private Long userId;

    // 语言类型（见 ProgramTypeEnum）
    private Integer programType;

    // 用户提交的原始代码
    private String userCode;

    // 拼装完成的待编译可执行完整代码
    private String completeCode;

    // 时间限制（毫秒）
    private Integer timeLimit;

    // 空间限制（MB）
    private Integer spaceLimit;

    // 题目难度（1:简单 2:中等 3:困难）
    private Integer difficulty;

    // 待执行的测试用例（按顺序喂入标准输入）
    private List<JudgeCaseDTO> cases;
}
