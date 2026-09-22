package cn.nuonuoya.api.ai.api;

import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiQuestionDraftDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// AI 服务内部接口契约（提供方：oj-ai，只做计算不写业务表；失败时返回 HTTP 4xx/5xx，由调用方转换为业务错误码）
public interface AiInternalApi {

    // 根据一句话描述生成题面草稿（调用方：oj-system，只读）
    @PostMapping("/ai/internal/question/draft")
    AiQuestionDraftVO generateQuestionDraft(@RequestBody AiQuestionDraftDTO draftDTO);

    // 根据题面与主函数生成测试用例输入，不含预期输出（调用方：oj-system，只读）
    @PostMapping("/ai/internal/question/case-inputs")
    AiCaseInputVO generateCaseInputs(@RequestBody AiCaseInputDTO caseInputDTO);
}
