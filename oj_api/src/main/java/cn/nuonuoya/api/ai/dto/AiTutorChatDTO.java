package cn.nuonuoya.api.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.util.List;

// 做题辅导对话请求（题面见 AiQuestionBaseDTO；上下文全部由调用方从数据库组装，AI 服务不读业务库）
@Getter
@Setter
@ToString(callSuper = true)
public class AiTutorChatDTO extends AiQuestionBaseDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 提问类型（见 AiTutorActionEnum）
    @NotNull(message = "提问类型不能为空")
    private Integer action;

    // 公开示例
    @Valid
    @Size(max = 10, message = "公开示例不能超过10组")
    private List<AiTutorSampleDTO> samples;

    // 用户编辑器中的当前代码（可为空）
    @Size(max = 10000, message = "代码不能超过10000个字符")
    private String userCode;

    // 被分析的提交（分析提交、解释编译错误、点评代码时携带）
    @Valid
    private AiTutorSubmissionDTO submission;

    // 最近的历史对话（按时间升序）
    @Valid
    @Size(max = 20, message = "历史对话不能超过20条")
    private List<AiTutorHistoryDTO> history;

    // 本次用户输入（快捷操作时为空）
    @Size(max = 500, message = "提问不能超过500个字符")
    private String message;
}
