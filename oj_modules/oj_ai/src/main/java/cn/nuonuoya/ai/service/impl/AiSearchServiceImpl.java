package cn.nuonuoya.ai.service.impl;

import cn.nuonuoya.ai.config.AiProperties;
import cn.nuonuoya.ai.exception.AiModelException;
import cn.nuonuoya.ai.service.AiSearchService;
import cn.nuonuoya.api.ai.dto.AiEmbeddingDTO;
import cn.nuonuoya.api.ai.vo.AiEmbeddingVO;
import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// 检索类 AI 能力实现
@Slf4j
@Service
public class AiSearchServiceImpl implements AiSearchService {

    // DashScope 文本向量类型：检索查询
    private static final String TEXT_TYPE_QUERY = "query";

    // DashScope 文本向量类型：被检索文档
    private static final String TEXT_TYPE_DOCUMENT = "document";

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private AiProperties aiProperties;

    // 计算文本向量，返回数量或维度与预期不符时视为失败
    @Override
    public AiEmbeddingVO embed(AiEmbeddingDTO embeddingDTO) {
        List<String> texts = embeddingDTO.getTexts();
        String model = aiProperties.getEmbeddingModel();
        int dimensions = aiProperties.getEmbeddingDimensions();
        long start = System.currentTimeMillis();
        EmbeddingResponse response;
        try {
            response = embeddingModel.call(new EmbeddingRequest(texts, DashScopeEmbeddingOptions.builder()
                    .model(model)
                    .dimensions(dimensions)
                    .textType(Boolean.TRUE.equals(embeddingDTO.getQuery()) ? TEXT_TYPE_QUERY : TEXT_TYPE_DOCUMENT)
                    .build()));
        } catch (Exception e) {
            log.warn("AI 向量计算失败, model = {}, 条数 = {}, error = {}", model, texts.size(), e.getMessage());
            throw new AiModelException("向量计算失败", e);
        }
        List<float[]> vectors = response.getResults().stream().map(Embedding::getOutput).toList();
        if (vectors.size() != texts.size() || vectors.stream().anyMatch(v -> v == null || v.length != dimensions)) {
            throw new AiModelException("向量计算结果与请求不符");
        }
        log.info("AI 向量计算完成, model = {}, 条数 = {}, 耗时 = {} ms", model, texts.size(), System.currentTimeMillis() - start);
        AiEmbeddingVO vo = new AiEmbeddingVO();
        vo.setModel(model);
        vo.setDimensions(dimensions);
        vo.setVectors(vectors);
        return vo;
    }
}
