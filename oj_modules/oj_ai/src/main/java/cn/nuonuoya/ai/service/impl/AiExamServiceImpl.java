package cn.nuonuoya.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.ai.config.AiProperties;
import cn.nuonuoya.ai.exception.AiModelException;
import cn.nuonuoya.ai.prompt.ExamPrompts;
import cn.nuonuoya.ai.service.AiExamService;
import cn.nuonuoya.ai.service.support.AiStructuredCaller;
import cn.nuonuoya.api.ai.dto.AiExamIntentDTO;
import cn.nuonuoya.api.ai.dto.AiExamSelectDTO;
import cn.nuonuoya.api.ai.vo.AiExamIntentVO;
import cn.nuonuoya.api.ai.vo.AiExamSelectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

// 竞赛帮建实现：只理解需求与挑题，候选的检索、校验与补齐由调用方负责
@Service
public class AiExamServiceImpl implements AiExamService {

    // 需求理解的采样温度
    private static final double INTENT_TEMPERATURE = 0.3;

    // 选题的采样温度
    private static final double SELECT_TEMPERATURE = 0.3;

    @Autowired
    private AiStructuredCaller aiStructuredCaller;

    @Autowired
    private AiProperties aiProperties;

    // 理解竞赛描述，缺少名称时视为失败
    @Override
    public AiExamIntentVO parseIntent(AiExamIntentDTO intentDTO) {
        AiExamIntentVO intent = aiStructuredCaller.call("竞赛需求理解", aiProperties.getExamModel(), INTENT_TEMPERATURE,
                ExamPrompts.INTENT_SYSTEM, ExamPrompts.intentUser(intentDTO.getDescription().trim()), AiExamIntentVO.class);
        if (StrUtil.isBlank(intent.getTitle())) {
            throw new AiModelException("竞赛需求理解：缺少竞赛名称");
        }
        intent.setTitle(intent.getTitle().trim());
        intent.setTopics(StrUtil.trimToEmpty(intent.getTopics()));
        return intent;
    }

    // 从候选中挑题（序号的合法性由调用方校验）
    @Override
    public AiExamSelectVO selectQuestions(AiExamSelectDTO selectDTO) {
        AiExamSelectVO result = aiStructuredCaller.call("竞赛选题", aiProperties.getExamModel(), SELECT_TEMPERATURE,
                ExamPrompts.SELECT_SYSTEM, ExamPrompts.selectUser(selectDTO), AiExamSelectVO.class);
        if (result.getIndexes() == null) {
            result.setIndexes(Collections.emptyList());
        }
        return result;
    }
}
