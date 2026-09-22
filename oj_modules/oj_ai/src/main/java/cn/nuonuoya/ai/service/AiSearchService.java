package cn.nuonuoya.ai.service;

import cn.nuonuoya.api.ai.dto.AiEmbeddingDTO;
import cn.nuonuoya.api.ai.vo.AiEmbeddingVO;

// 检索类 AI 能力
public interface AiSearchService {

    // 计算文本向量
    AiEmbeddingVO embed(AiEmbeddingDTO embeddingDTO);
}
