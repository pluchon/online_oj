package cn.nuonuoya.elastic.repository;

import cn.nuonuoya.elastic.doc.QuestionDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

// 题目 Elasticsearch 仓库（条件检索使用 ElasticsearchOperations）
@Repository
public interface QuestionRepository extends ElasticsearchRepository<QuestionDoc, Long> {
}
