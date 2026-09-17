package cn.nuonuoya.elastic.repository;

import cn.nuonuoya.elastic.doc.QuestionDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

// 题目Elasticsearch持久化仓库接口
@Repository
public interface QuestionRepository extends ElasticsearchRepository<QuestionDoc, Long> {

    // 根据难度分页查询题目
    Page<QuestionDoc> findByDifficulty(Integer difficulty, Pageable pageable);

    // 根据标题或内容包含关键字分页查询
    Page<QuestionDoc> findByTitleContainingOrContentContaining(String title, String content, Pageable pageable);
}
