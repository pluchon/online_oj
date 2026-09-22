package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.QuestionAiCaseDTO;
import cn.nuonuoya.system.dto.QuestionAiDraftDTO;
import cn.nuonuoya.system.vo.QuestionAiCaseVO;
import cn.nuonuoya.system.vo.QuestionAiDraftVO;

// AI 辅助出题
public interface QuestionAiService {

    // 生成题面草稿（只回填表单，不落库）
    QuestionAiDraftVO generateDraft(QuestionAiDraftDTO draftDTO);

    // 生成测试用例预览：模型给输入，标程在判题沙箱中运行得到预期输出（不落库）
    QuestionAiCaseVO generateCases(QuestionAiCaseDTO caseDTO);
}
