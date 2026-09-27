package cn.nuonuoya.ai.service;

import cn.nuonuoya.api.ai.dto.AiHardAnalysisDTO;
import cn.nuonuoya.api.ai.vo.AiHardAnalysisVO;

// 管理端数据分析业务接口
public interface AiAnalysisService {

    // 难题分析：归纳薄弱点与错误类型，判断可疑题是否出题有误
    AiHardAnalysisVO analyzeHardQuestions(AiHardAnalysisDTO analysisDTO);
}
