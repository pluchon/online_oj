package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.QuestionAiCaseDTO;
import cn.nuonuoya.system.dto.QuestionAiDraftDTO;
import cn.nuonuoya.system.dto.QuestionAiEditorialDTO;
import cn.nuonuoya.system.dto.QuestionAiSolutionDTO;
import cn.nuonuoya.system.vo.QuestionAiCaseVO;
import cn.nuonuoya.system.vo.QuestionAiDraftVO;
import cn.nuonuoya.system.vo.QuestionAiEditorialVO;
import cn.nuonuoya.system.vo.QuestionAiSolutionVO;

// AI 辅助出题
public interface QuestionAiService {

    // 生成题面草稿（只回填表单，不落库）
    QuestionAiDraftVO generateDraft(QuestionAiDraftDTO draftDTO);

    // 生成测试用例预览：模型给输入，标程在判题沙箱中运行得到预期输出（不落库）
    QuestionAiCaseVO generateCases(QuestionAiCaseDTO caseDTO);

    // 生成解法示例（只展示，不落库）
    QuestionAiSolutionVO generateSolution(QuestionAiSolutionDTO solutionDTO);

    // 生成题解草稿（回填题解编辑框，不落库）
    QuestionAiEditorialVO generateEditorial(QuestionAiEditorialDTO editorialDTO);
}
