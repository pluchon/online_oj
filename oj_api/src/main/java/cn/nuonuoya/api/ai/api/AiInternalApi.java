package cn.nuonuoya.api.ai.api;

import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiEditorialDTO;
import cn.nuonuoya.api.ai.dto.AiExamIntentDTO;
import cn.nuonuoya.api.ai.dto.AiExamSelectDTO;
import cn.nuonuoya.api.ai.dto.AiQuestionDraftDTO;
import cn.nuonuoya.api.ai.dto.AiSolutionDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputVO;
import cn.nuonuoya.api.ai.vo.AiEditorialVO;
import cn.nuonuoya.api.ai.vo.AiExamIntentVO;
import cn.nuonuoya.api.ai.vo.AiExamSelectVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import cn.nuonuoya.api.ai.vo.AiSolutionVO;
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

    // 根据题面生成常见解法（用作解法示例，并作为生成用例时的标程；调用方：oj-system，只读）
    @PostMapping("/ai/internal/question/solution")
    AiSolutionVO generateSolution(@RequestBody AiSolutionDTO solutionDTO);

    // 根据题面（与可选的参考解法）生成题解草稿：思路、复杂度与代码（调用方：oj-system，只读）
    @PostMapping("/ai/internal/question/editorial")
    AiEditorialVO generateEditorial(@RequestBody AiEditorialDTO editorialDTO);

    // 理解竞赛描述：名称、主题关键词与明确给出的题数（调用方：oj-system，只读）
    @PostMapping("/ai/internal/exam/intent")
    AiExamIntentVO parseExamIntent(@RequestBody AiExamIntentDTO intentDTO);

    // 从候选题目中按各难度数量挑题（调用方：oj-system，只读）
    @PostMapping("/ai/internal/exam/select")
    AiExamSelectVO selectExamQuestions(@RequestBody AiExamSelectDTO selectDTO);
}
