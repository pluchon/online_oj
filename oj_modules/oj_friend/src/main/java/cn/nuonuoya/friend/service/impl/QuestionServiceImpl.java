package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.elastic.doc.QuestionDoc;
import cn.nuonuoya.elastic.repository.QuestionRepository;
import cn.nuonuoya.friend.cache.QuestionCacheManager;
import cn.nuonuoya.friend.converter.QuestionConverter;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.dto.QuestionQueryDTO;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.service.QuestionService;
import cn.nuonuoya.friend.vo.QuestionPreNextVO;
import cn.nuonuoya.friend.vo.QuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// 题目业务服务实现类
@Service
public class QuestionServiceImpl implements QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionServiceImpl.class);

    // 注入ES持久层仓库
    @Autowired
    private QuestionRepository questionRepository;

    // 注入MySQL题目Mapper
    @Autowired
    private QuestionMapper questionMapper;

    // 注入ES操作模板
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    // 注入题目缓存管理器
    @Autowired
    private QuestionCacheManager questionCacheManager;

    // 分页全文检索题目列表
    @Override
    public TableDataResult<QuestionVO> search(QuestionQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new QuestionQueryDTO();
        }

        try {
            // 初次若ES无数据则自动同步
            long esTotal = questionRepository.count();
            if (esTotal == 0) {
                syncAllQuestionsToEs();
            }

            // 构造ES全文检索条件
            Criteria criteria = null;

            // 标题或内容全文检索切词匹配
            if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
                String kw = queryDTO.getKeyword().trim();
                criteria = Criteria.where("title").matches(kw)
                        .or(Criteria.where("content").matches(kw));
            }

            // 难度筛选
            if (queryDTO.getDifficulty() != null && queryDTO.getDifficulty() > 0) {
                Criteria diffCriteria = Criteria.where("difficulty").is(queryDTO.getDifficulty());
                if (criteria == null) {
                    criteria = diffCriteria;
                } else {
                    criteria = criteria.and(diffCriteria);
                }
            }

            // 分页参数装配
            int pageNum = Math.max(1, queryDTO.getPageNum());
            int pageSize = Math.max(1, queryDTO.getPageSize());
            Pageable pageable = PageRequest.of(pageNum - 1, pageSize);

            Query esQuery;
            if (criteria != null) {
                esQuery = new CriteriaQuery(criteria).setPageable(pageable);
            } else {
                esQuery = new CriteriaQuery(new Criteria()).setPageable(pageable);
            }

            // 执行ES查询
            SearchHits<QuestionDoc> searchHits = elasticsearchOperations.search(esQuery, QuestionDoc.class);
            long total = searchHits.getTotalHits();
            List<QuestionDoc> docList = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            List<QuestionVO> voList = QuestionConverter.toVOListFromDoc(docList);
            return TableDataResult.success(voList, total);

        } catch (Exception e) {
            // ES异常时降级至MySQL查询保证可用性
            log.error("Elasticsearch题目检索异常，执行MySQL降级查询: {}", e.getMessage(), e);
            return searchFromMySQL(queryDTO);
        }
    }

    // 查询题目详情
    @Override
    public QuestionVO getDetail(Long questionId) {
        if (questionId == null) {
            return null;
        }

        // 优先从ES读取
        try {
            Optional<QuestionDoc> docOpt = questionRepository.findById(questionId);
            if (docOpt.isPresent()) {
                return QuestionConverter.toVO(docOpt.get());
            }
        } catch (Exception e) {
            log.warn("从ES读取题目详情失败，降级查库: {}", e.getMessage());
        }

        // 兜底查MySQL
        TbQuestion entity = questionMapper.selectById(questionId);
        return QuestionConverter.toVO(entity);
    }

    // 全量同步MySQL题目数据至ES索引
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncAllQuestionsToEs() {
        List<TbQuestion> list = questionMapper.selectList(null);
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        List<QuestionDoc> docList = QuestionConverter.toDocList(list);
        questionRepository.saveAll(docList);
        log.info("成功同步 {} 道题目至Elasticsearch索引", docList.size());
        return docList.size();
    }

    // MySQL数据库降级分页检索
    private TableDataResult<QuestionVO> searchFromMySQL(QuestionQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<TbQuestion> lqw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword().trim();
            lqw.and(w -> w.like(TbQuestion::getTitle, kw).or().like(TbQuestion::getContent, kw));
        }
        if (queryDTO.getDifficulty() != null && queryDTO.getDifficulty() > 0) {
            lqw.eq(TbQuestion::getDifficulty, queryDTO.getDifficulty());
        }
        lqw.orderByDesc(TbQuestion::getQuestionId);

        List<TbQuestion> list = questionMapper.selectList(lqw);
        if (CollUtil.isEmpty(list)) {
            return TableDataResult.empty();
        }
        long total = new PageInfo<>(list).getTotal();
        List<QuestionVO> voList = list.stream()
                .map(QuestionConverter::toVO)
                .collect(Collectors.toList());
        return TableDataResult.success(voList, total);
    }

    // 获取题目上一题与下一题导航信息
    @Override
    public QuestionPreNextVO getPreAndNext(Long questionId, Long examId) {
        return questionCacheManager.getPreAndNextQuestionId(questionId, examId);
    }

    // 获取首道题目ID
    @Override
    public Long getFirstQuestionId(Long examId) {
        return questionCacheManager.getFirstQuestionId(examId);
    }
}

