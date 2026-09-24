package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.dto.AiEmbeddingDTO;
import cn.nuonuoya.api.ai.vo.AiEmbeddingVO;
import cn.nuonuoya.friend.constants.SentinelResources;
import cn.nuonuoya.sentinel.SentinelGuard;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

// AI 向量调用边界（失败、被限流或熔断时返回 null，语义检索与向量写入按"没有向量"处理，不影响关键词检索）
@Slf4j
@Component
public class AiSearchClient {

    @Autowired
    private AiSearchFeignClient aiSearchFeignClient;

    // 计算一批被检索文本的向量（最多 10 条），失败返回 null
    public List<float[]> embedDocuments(List<String> texts) {
        return embed(texts, false);
    }

    // 计算检索查询词的向量，失败返回 null
    public float[] embedQuery(String text) {
        List<float[]> vectors = embed(List.of(text), true);
        return vectors == null ? null : vectors.get(0);
    }

    // 调用向量接口并校验返回数量
    private List<float[]> embed(List<String> texts, boolean query) {
        AiEmbeddingDTO dto = new AiEmbeddingDTO();
        dto.setTexts(texts);
        dto.setQuery(query);
        try {
            AiEmbeddingVO vo = SentinelGuard.call(SentinelResources.AI_EMBEDDING, () -> aiSearchFeignClient.embed(dto));
            if (vo != null && vo.getVectors() != null && vo.getVectors().size() == texts.size()) {
                return vo.getVectors();
            }
            log.warn("AI 向量接口返回数量不符, 请求 = {}", texts.size());
        } catch (BlockException e) {
            log.warn("AI 向量调用被限流或熔断，按没有向量处理, rule = {}", e.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("调用 AI 向量接口失败, error = {}", e.getMessage());
        }
        return null;
    }
}
