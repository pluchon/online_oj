package cn.nuonuoya.friend.search;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.nuonuoya.elastic.doc.QuestionDoc;
import cn.nuonuoya.elastic.repository.QuestionRepository;
import cn.nuonuoya.friend.client.AiSearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 题目语义检索：维护题目向量，并在关键词无结果时与相似题推荐中做 kNN 检索
@Slf4j
@Component
public class QuestionSemanticSearcher {

    // 向量字段名（题目文档中的 embedding 字段）
    public static final String EMBEDDING_FIELD = "embedding";

    // 难度字段名（语义检索的难度过滤）
    private static final String DIFFICULTY_FIELD = "difficulty";

    // 单次向量计算的最大条数（DashScope 文本向量接口限制）
    private static final int EMBED_BATCH_SIZE = 10;

    // 参与向量计算的文本最大长度
    private static final int EMBED_TEXT_LIMIT = 2000;

    // kNN 候选数量相对返回数量的倍数
    private static final int CANDIDATE_FACTOR = 10;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AiSearchClient aiSearchClient;

    // 语义检索的最低余弦相似度，低于它的结果视为无关，可在 Nacos 中覆盖
    @Value("${oj.ai.search.min-similarity:0.55}")
    private float minSimilarity;

    // 确保索引存在且映射包含向量字段（旧索引补充新字段）
    public void ensureMapping() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(QuestionDoc.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        } else {
            indexOps.putMapping();
        }
    }

    // 为待写入的题目填充向量：文本未变化的复用已有向量，其余批量计算；计算失败的题目不带向量写入
    public void fillEmbeddings(List<QuestionDoc> docs) {
        if (CollUtil.isEmpty(docs)) {
            return;
        }
        Map<Long, QuestionDoc> existing = new HashMap<>();
        questionRepository.findAllById(docs.stream().map(QuestionDoc::getQuestionId).toList())
                .forEach(doc -> existing.put(doc.getQuestionId(), doc));

        List<QuestionDoc> pending = new ArrayList<>();
        for (QuestionDoc doc : docs) {
            String hash = SecureUtil.md5(embeddingText(doc));
            doc.setEmbeddingHash(hash);
            QuestionDoc old = existing.get(doc.getQuestionId());
            if (old != null && old.getEmbedding() != null && hash.equals(old.getEmbeddingHash())) {
                doc.setEmbedding(old.getEmbedding());
            } else {
                pending.add(doc);
            }
        }

        int failed = 0;
        for (int i = 0; i < pending.size(); i += EMBED_BATCH_SIZE) {
            List<QuestionDoc> batch = pending.subList(i, Math.min(i + EMBED_BATCH_SIZE, pending.size()));
            List<float[]> vectors = aiSearchClient.embedDocuments(batch.stream().map(this::embeddingText).toList());
            for (int j = 0; j < batch.size(); j++) {
                if (vectors == null) {
                    // 未取得向量时不保存摘要，下次同步会重试
                    batch.get(j).setEmbeddingHash(null);
                    failed++;
                } else {
                    batch.get(j).setEmbedding(vectors.get(j));
                }
            }
        }
        log.info("题目向量：复用 {} 道，新计算 {} 道，失败 {} 道", docs.size() - pending.size(), pending.size() - failed, failed);
    }

    // 语义检索：查询词向量化后做 kNN，过滤低相似度结果；向量服务不可用时返回空列表
    public List<QuestionDoc> search(String keyword, Integer difficulty, int size) {
        float[] vector = aiSearchClient.embedQuery(StrUtil.maxLength(keyword.trim(), EMBED_TEXT_LIMIT));
        if (vector == null) {
            return Collections.emptyList();
        }
        List<Query> filters = new ArrayList<>();
        if (difficulty != null && difficulty > 0) {
            filters.add(Query.of(q -> q.term(t -> t.field(DIFFICULTY_FIELD).value(difficulty))));
        }
        return knn(vector, filters, size);
    }

    // 相似题：以题目自身向量做 kNN，排除自身与指定题目；题目尚无向量时返回空列表
    public List<QuestionDoc> similar(Long questionId, Collection<Long> excludeIds, int size) {
        QuestionDoc doc = questionRepository.findById(questionId).orElse(null);
        if (doc == null || doc.getEmbedding() == null) {
            return Collections.emptyList();
        }
        List<String> excluded = new ArrayList<>();
        excluded.add(String.valueOf(questionId));
        excludeIds.forEach(id -> excluded.add(String.valueOf(id)));
        Query exclude = Query.of(q -> q.bool(b -> b.mustNot(m -> m.ids(i -> i.values(excluded)))));
        return knn(doc.getEmbedding(), List.of(exclude), size);
    }

    // 执行 kNN 检索（结果不返回向量字段）
    private List<QuestionDoc> knn(float[] vector, List<Query> filters, int size) {
        List<Float> queryVector = new ArrayList<>(vector.length);
        for (float v : vector) {
            queryVector.add(v);
        }
        NativeQuery query = NativeQuery.builder()
                .withKnnSearches(k -> k.field(EMBEDDING_FIELD)
                        .queryVector(queryVector)
                        .k(size)
                        .numCandidates(Math.max(size * CANDIDATE_FACTOR, 50))
                        .similarity(minSimilarity)
                        .filter(filters))
                .withSourceFilter(new FetchSourceFilterBuilder().withExcludes(EMBEDDING_FIELD).build())
                .withMaxResults(size)
                .build();
        return elasticsearchOperations.search(query, QuestionDoc.class).getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }

    // 参与向量计算的文本：标题 + 描述
    private String embeddingText(QuestionDoc doc) {
        return StrUtil.maxLength(StrUtil.nullToEmpty(doc.getTitle()) + "\n" + StrUtil.nullToEmpty(doc.getContent()), EMBED_TEXT_LIMIT);
    }
}
