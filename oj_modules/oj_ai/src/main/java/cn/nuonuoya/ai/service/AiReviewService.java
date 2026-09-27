package cn.nuonuoya.ai.service;

import cn.nuonuoya.api.ai.dto.AiExamReviewDTO;
import cn.nuonuoya.api.ai.vo.AiExamReviewVO;

// 赛后复盘业务接口
public interface AiReviewService {

    // 写逐题点评与整体总结
    AiExamReviewVO reviewExam(AiExamReviewDTO reviewDTO);
}
