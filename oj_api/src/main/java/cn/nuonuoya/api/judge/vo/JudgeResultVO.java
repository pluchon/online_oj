package cn.nuonuoya.api.judge.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 判题服务执行结果响应值对象
@Getter
@Setter
@ToString
public class JudgeResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 提交记录ID
    private Long submitId;

    // 状态编码（见 JudgeStatusEnum）
    private Integer status;

    // 状态英文标识（如 Accepted, Wrong Answer, Time Limit Exceeded）
    private String statusDesc;

    // 是否通过（见 JudgePassEnum）
    private Integer pass;

    // 本次提交最终得分
    private Integer score;

    // 通过的用例数
    private Integer passCount;

    // 总用例数
    private Integer totalCount;

    // 实际执行耗时（毫秒）
    private Long timeCost;

    // 实际内存占用（MB）
    private Long memoryCost;

    // 执行回显详细信息（编译错误、运行异常等原始输出）
    private String exeMessage;

    // 首个未通过用例ID
    private Long failCaseId;

    // 首个未通过用例的实际输出
    private String failOutput;

    // 逐用例执行结果（与请求用例顺序一致）
    private List<JudgeCaseResultVO> caseResults;
}
