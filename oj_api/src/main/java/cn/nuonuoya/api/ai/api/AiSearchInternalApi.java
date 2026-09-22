package cn.nuonuoya.api.ai.api;

import cn.nuonuoya.api.ai.dto.AiEmbeddingDTO;
import cn.nuonuoya.api.ai.vo.AiEmbeddingVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// AI 检索类内部接口（调用方：oj-friend 题目索引与语义检索；只做计算不写业务表）
public interface AiSearchInternalApi {

    // 计算文本向量（返回顺序与输入一致）
    @PostMapping("/ai/internal/embedding")
    AiEmbeddingVO embed(@RequestBody AiEmbeddingDTO embeddingDTO);
}
