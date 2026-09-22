package cn.nuonuoya.ai.service;

import cn.nuonuoya.api.ai.dto.AiExamIntentDTO;
import cn.nuonuoya.api.ai.dto.AiExamSelectDTO;
import cn.nuonuoya.api.ai.vo.AiExamIntentVO;
import cn.nuonuoya.api.ai.vo.AiExamSelectVO;

// 竞赛帮建 AI 能力
public interface AiExamService {

    // 理解竞赛描述
    AiExamIntentVO parseIntent(AiExamIntentDTO intentDTO);

    // 从候选中挑题
    AiExamSelectVO selectQuestions(AiExamSelectDTO selectDTO);
}
