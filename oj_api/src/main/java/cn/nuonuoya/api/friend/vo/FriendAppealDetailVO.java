package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

// 申诉详情（公共字段见 FriendAppealBaseVO，另含申诉理由、AI 分析与被申诉提交的代码和逐用例结果）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendAppealDetailVO extends FriendAppealBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 申诉理由
    private String reason;

    // AI 初审分析
    private String aiAnalysis;

    // 提交代码
    private String userCode;

    // 语言类型（见 ProgramTypeEnum）
    private Integer programType;

    // 当前判题结论（改判后为通过）
    private Integer judgeStatus;

    // 通过用例数
    private Integer passCount;

    // 总用例数
    private Integer totalCount;

    // 执行耗时（毫秒）
    private Integer timeCost;

    // 执行回显
    private String exeMessage;

    // 提交时间
    private LocalDateTime submitTime;

    // 逐用例状态（按用例顺序，1: 通过 0: 未通过 -: 未执行）
    private String caseStates;

    // 逐用例结果（有记录时与 caseStates 顺序一致；早期提交为空）
    private List<FriendCaseResultVO> caseResults;

    // 首个未通过用例ID（早期提交只有这一条输出）
    private Long failCaseId;

    // 首个未通过用例的实际输出
    private String failOutput;
}
