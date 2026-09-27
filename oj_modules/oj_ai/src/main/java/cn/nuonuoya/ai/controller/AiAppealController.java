package cn.nuonuoya.ai.controller;

import cn.nuonuoya.ai.service.AiAppealService;
import cn.nuonuoya.api.ai.api.AiAppealInternalApi;
import cn.nuonuoya.api.ai.dto.AiAppealReviewDTO;
import cn.nuonuoya.api.ai.vo.AiAppealReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 申诉 AI 初审内部接口控制器（不经网关暴露）
@RestController
public class AiAppealController implements AiAppealInternalApi {

    @Autowired
    private AiAppealService aiAppealService;

    /** 申诉初审 */
    @Override
    public AiAppealReviewVO reviewAppeal(@RequestBody AiAppealReviewDTO reviewDTO) {
        return aiAppealService.review(reviewDTO);
    }
}
