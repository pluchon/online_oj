package cn.nuonuoya.friend.client;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.ai.dto.AiExamReviewDTO;
import cn.nuonuoya.api.ai.vo.AiExamReviewVO;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 赛后复盘调用边界：服务不可用、超时或没有总结时抛出 AI 服务繁忙，不保存残缺的复盘
@Slf4j
@Component
public class AiReviewClient {

    @Autowired
    private AiReviewFeignClient aiReviewFeignClient;

    // 调用 AI 写复盘
    public AiExamReviewVO review(AiExamReviewDTO reviewDTO) {
        try {
            AiExamReviewVO result = aiReviewFeignClient.reviewExam(reviewDTO);
            if (result != null && StrUtil.isNotBlank(result.getSummary()) && result.getQuestions() != null) {
                return result;
            }
            log.warn("AI 赛后复盘返回没有总结");
        } catch (Exception e) {
            log.error("调用 AI 赛后复盘失败, error = {}", e.getMessage());
        }
        throw new ServiceException(ResultCode.FAILED_AI_BUSY);
    }
}
