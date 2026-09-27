package cn.nuonuoya.api.ai.api;

import cn.nuonuoya.api.ai.dto.AiExamReviewDTO;
import cn.nuonuoya.api.ai.vo.AiExamReviewVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// 赛后复盘内部接口契约（提供方：oj-ai，只做计算不写库；调用方：oj-friend 学员打开复盘时；失败时返回 HTTP 4xx/5xx）
public interface AiReviewInternalApi {

    // 根据学员这场的成绩与逐题情况，写逐题点评与整体总结
    @PostMapping("/ai/internal/review/exam")
    AiExamReviewVO reviewExam(@RequestBody AiExamReviewDTO reviewDTO);
}
