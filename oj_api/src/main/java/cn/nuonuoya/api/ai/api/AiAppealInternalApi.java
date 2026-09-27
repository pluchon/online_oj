package cn.nuonuoya.api.ai.api;

import cn.nuonuoya.api.ai.dto.AiAppealReviewDTO;
import cn.nuonuoya.api.ai.vo.AiAppealReviewVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// 申诉 AI 初审内部接口契约（提供方：oj-ai，只做判断不写库；调用方：oj-friend 学员发起申诉前；失败时返回 HTTP 4xx/5xx）
public interface AiAppealInternalApi {

    // 判断一次未通过的提交是否可能被判错（用例或判题有误），返回结论与只给管理员看的分析
    @PostMapping("/ai/internal/appeal/review")
    AiAppealReviewVO reviewAppeal(@RequestBody AiAppealReviewDTO reviewDTO);
}
