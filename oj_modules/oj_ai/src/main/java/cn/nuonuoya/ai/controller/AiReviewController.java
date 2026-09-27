package cn.nuonuoya.ai.controller;

import cn.nuonuoya.ai.service.AiReviewService;
import cn.nuonuoya.api.ai.api.AiReviewInternalApi;
import cn.nuonuoya.api.ai.dto.AiExamReviewDTO;
import cn.nuonuoya.api.ai.vo.AiExamReviewVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 赛后复盘内部接口控制器（不经网关暴露）
@RestController
public class AiReviewController implements AiReviewInternalApi {

    @Autowired
    private AiReviewService aiReviewService;

    /** 赛后复盘 */
    @Override
    public AiExamReviewVO reviewExam(@Valid @RequestBody AiExamReviewDTO reviewDTO) {
        return aiReviewService.reviewExam(reviewDTO);
    }
}
