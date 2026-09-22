package cn.nuonuoya.ai.controller;

import cn.nuonuoya.ai.service.AiSearchService;
import cn.nuonuoya.api.ai.api.AiSearchInternalApi;
import cn.nuonuoya.api.ai.dto.AiEmbeddingDTO;
import cn.nuonuoya.api.ai.vo.AiEmbeddingVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// AI 检索类内部接口控制器（不经网关暴露）
@RestController
public class AiSearchController implements AiSearchInternalApi {

    @Autowired
    private AiSearchService aiSearchService;

    /** 计算文本向量 */
    @Override
    public AiEmbeddingVO embed(@Valid @RequestBody AiEmbeddingDTO embeddingDTO) {
        return aiSearchService.embed(embeddingDTO);
    }
}
