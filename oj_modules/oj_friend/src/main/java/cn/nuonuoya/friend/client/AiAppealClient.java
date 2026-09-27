package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.dto.AiAppealReviewDTO;
import cn.nuonuoya.api.ai.vo.AiAppealReviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 申诉 AI 初审调用边界：服务不可用、超时或返回没有结论时返回 null，由调用方归还次数并提示稍后重试
@Slf4j
@Component
public class AiAppealClient {

    @Autowired
    private AiAppealFeignClient aiAppealFeignClient;

    // 调用 AI 初审，失败返回 null
    public AiAppealReviewVO review(AiAppealReviewDTO reviewDTO) {
        try {
            AiAppealReviewVO result = aiAppealFeignClient.reviewAppeal(reviewDTO);
            if (result != null && result.getSuspicious() != null) {
                return result;
            }
            log.warn("AI 初审返回没有结论");
        } catch (Exception e) {
            log.error("调用 AI 初审失败, error = {}", e.getMessage());
        }
        return null;
    }
}
