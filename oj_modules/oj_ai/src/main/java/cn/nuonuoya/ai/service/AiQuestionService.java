package cn.nuonuoya.ai.service;

import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiEditorialDTO;
import cn.nuonuoya.api.ai.dto.AiQuestionDraftDTO;
import cn.nuonuoya.api.ai.dto.AiSolutionDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputVO;
import cn.nuonuoya.api.ai.vo.AiEditorialVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import cn.nuonuoya.api.ai.vo.AiSolutionVO;

// 出题类 AI 能力
public interface AiQuestionService {

    // 根据一句话描述生成题面草稿
    AiQuestionDraftVO generateQuestionDraft(AiQuestionDraftDTO draftDTO);

    // 根据题面与主函数生成测试用例输入（不含预期输出）
    AiCaseInputVO generateCaseInputs(AiCaseInputDTO caseInputDTO);

    // 根据题面生成常见解法
    AiSolutionVO generateSolution(AiSolutionDTO solutionDTO);

    // 根据题面（与可选的参考解法）生成题解草稿
    AiEditorialVO generateEditorial(AiEditorialDTO editorialDTO);
}
