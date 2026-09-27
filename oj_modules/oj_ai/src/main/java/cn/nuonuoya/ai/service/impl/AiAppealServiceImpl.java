package cn.nuonuoya.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.ai.config.AiProperties;
import cn.nuonuoya.ai.prompt.AppealPrompts;
import cn.nuonuoya.ai.service.AiAppealService;
import cn.nuonuoya.ai.service.support.AiStructuredCaller;
import cn.nuonuoya.api.ai.dto.AiAppealReviewDTO;
import cn.nuonuoya.api.ai.vo.AiAppealReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 申诉 AI 初审实现（结构化输出；模型没给出明确结论时按「未发现判错」处理）
@Service
public class AiAppealServiceImpl implements AiAppealService {

    // 分析最多保留的字数
    private static final int ANALYSIS_LIMIT = 1000;

    @Autowired
    private AiStructuredCaller aiStructuredCaller;

    @Autowired
    private AiProperties aiProperties;

    // 调用模型判断，结论为空时视为 false，分析截断到固定长度
    @Override
    public AiAppealReviewVO review(AiAppealReviewDTO reviewDTO) {
        AiAppealReviewVO result = aiStructuredCaller.call("申诉初审", aiProperties.getQuestionModel(),
                aiProperties.getAppealTemperature(), AppealPrompts.SYSTEM, AppealPrompts.user(reviewDTO), AiAppealReviewVO.class);
        result.setSuspicious(Boolean.TRUE.equals(result.getSuspicious()));
        result.setAnalysis(StrUtil.maxLength(StrUtil.trimToEmpty(result.getAnalysis()), ANALYSIS_LIMIT));
        return result;
    }
}
