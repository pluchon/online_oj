package cn.nuonuoya.ai.service;

import cn.nuonuoya.api.ai.dto.AiAppealReviewDTO;
import cn.nuonuoya.api.ai.vo.AiAppealReviewVO;

// 申诉 AI 初审业务接口
public interface AiAppealService {

    // 判断一次未通过的提交是否可能被判错
    AiAppealReviewVO review(AiAppealReviewDTO reviewDTO);
}
