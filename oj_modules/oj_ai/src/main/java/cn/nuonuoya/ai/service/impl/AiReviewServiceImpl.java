package cn.nuonuoya.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.ai.config.AiProperties;
import cn.nuonuoya.ai.prompt.ReviewPrompts;
import cn.nuonuoya.ai.service.AiReviewService;
import cn.nuonuoya.ai.service.support.AiStructuredCaller;
import cn.nuonuoya.api.ai.dto.AiExamReviewDTO;
import cn.nuonuoya.api.ai.dto.AiExamReviewQuestionDTO;
import cn.nuonuoya.api.ai.vo.AiExamReviewCommentVO;
import cn.nuonuoya.api.ai.vo.AiExamReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 赛后复盘实现（结构化输出；只保留有提交的题的点评，每题一条，文字去掉多余开头并截断）
@Service
public class AiReviewServiceImpl implements AiReviewService {

    // 总结最多保留的字数
    private static final int SUMMARY_LIMIT = 300;

    // 单题点评最多保留的字数
    private static final int COMMENT_LIMIT = 200;

    // 模型偶尔仍会带上的「AI：」「点评：」开头
    private static final Pattern PREFIX = Pattern.compile("^\\s*(AI|点评)\\s*[：:]\\s*", Pattern.CASE_INSENSITIVE);

    @Autowired
    private AiStructuredCaller aiStructuredCaller;

    @Autowired
    private AiProperties aiProperties;

    // 调用模型，清理空白、越界与没有提交的题
    @Override
    public AiExamReviewVO reviewExam(AiExamReviewDTO reviewDTO) {
        AiExamReviewVO result = aiStructuredCaller.call("赛后复盘", aiProperties.getQuestionModel(),
                aiProperties.getReviewTemperature(), ReviewPrompts.SYSTEM, ReviewPrompts.user(reviewDTO), AiExamReviewVO.class);
        result.setSummary(clean(result.getSummary(), SUMMARY_LIMIT));
        Set<Integer> submitted = CollUtil.emptyIfNull(reviewDTO.getQuestions()).stream()
                .filter(q -> q.getSubmitCount() != null && q.getSubmitCount() > 0)
                .map(AiExamReviewQuestionDTO::getIndex)
                .collect(Collectors.toSet());
        Map<Integer, AiExamReviewCommentVO> commentByIndex = new LinkedHashMap<>();
        for (AiExamReviewCommentVO item : CollUtil.emptyIfNull(result.getQuestions())) {
            if (item != null && submitted.contains(item.getIndex()) && StrUtil.isNotBlank(item.getComment())) {
                item.setComment(clean(item.getComment(), COMMENT_LIMIT));
                commentByIndex.putIfAbsent(item.getIndex(), item);
            }
        }
        result.setQuestions(new ArrayList<>(commentByIndex.values()));
        return result;
    }

    // 去掉首尾空白与多余开头，并截断到固定长度
    private String clean(String text, int limit) {
        return StrUtil.maxLength(PREFIX.matcher(StrUtil.trimToEmpty(text)).replaceFirst(""), limit);
    }
}
