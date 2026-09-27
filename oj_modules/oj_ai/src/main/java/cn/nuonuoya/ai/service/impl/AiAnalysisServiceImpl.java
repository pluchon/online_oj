package cn.nuonuoya.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.ai.config.AiProperties;
import cn.nuonuoya.ai.prompt.AnalysisPrompts;
import cn.nuonuoya.ai.service.AiAnalysisService;
import cn.nuonuoya.ai.service.support.AiStructuredCaller;
import cn.nuonuoya.api.ai.dto.AiHardAnalysisDTO;
import cn.nuonuoya.api.ai.dto.AiHardSuspectDTO;
import cn.nuonuoya.api.ai.vo.AiHardAnalysisVO;
import cn.nuonuoya.api.ai.vo.AiHardSuspectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 管理端数据分析实现（结构化输出；文字截断到固定长度，可疑题只保留请求里有的序号且每题一条）
@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    // 结论最多保留的字数
    private static final int SUMMARY_LIMIT = 300;

    // 单题判断最多保留的字数
    private static final int COMMENT_LIMIT = 200;

    // 模型偶尔仍会带上的「AI：」开头
    private static final Pattern AI_PREFIX = Pattern.compile("^\\s*AI\\s*[：:]\\s*", Pattern.CASE_INSENSITIVE);

    @Autowired
    private AiStructuredCaller aiStructuredCaller;

    @Autowired
    private AiProperties aiProperties;

    // 调用模型归纳，清理空白与越界的结果
    @Override
    public AiHardAnalysisVO analyzeHardQuestions(AiHardAnalysisDTO analysisDTO) {
        AiHardAnalysisVO result = aiStructuredCaller.call("难题分析", aiProperties.getQuestionModel(),
                aiProperties.getAnalysisTemperature(), AnalysisPrompts.SYSTEM, AnalysisPrompts.user(analysisDTO),
                AiHardAnalysisVO.class);
        result.setWeakSummary(clean(result.getWeakSummary(), SUMMARY_LIMIT));
        result.setVerdictSummary(clean(result.getVerdictSummary(), SUMMARY_LIMIT));
        Set<Integer> indexes = CollUtil.emptyIfNull(analysisDTO.getSuspects()).stream()
                .map(AiHardSuspectDTO::getIndex)
                .collect(Collectors.toSet());
        Map<Integer, AiHardSuspectVO> suspectByIndex = new LinkedHashMap<>();
        for (AiHardSuspectVO item : CollUtil.emptyIfNull(result.getSuspects())) {
            if (item != null && indexes.contains(item.getIndex()) && StrUtil.isNotBlank(item.getComment())) {
                item.setComment(clean(item.getComment(), COMMENT_LIMIT));
                suspectByIndex.putIfAbsent(item.getIndex(), item);
            }
        }
        result.setSuspects(new ArrayList<>(suspectByIndex.values()));
        return result;
    }

    // 去掉首尾空白与「AI：」开头，并截断到固定长度
    private String clean(String text, int limit) {
        return StrUtil.maxLength(AI_PREFIX.matcher(StrUtil.trimToEmpty(text)).replaceFirst(""), limit);
    }
}
