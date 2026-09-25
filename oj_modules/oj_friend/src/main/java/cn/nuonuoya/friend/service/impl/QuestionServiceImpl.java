package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.security.utils.SecurityUtils;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.elastic.doc.QuestionDoc;
import cn.nuonuoya.elastic.repository.QuestionRepository;
import cn.nuonuoya.api.friend.dto.FriendQuestionCandidateQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendQuestionCandidateVO;
import cn.nuonuoya.friend.cache.QuestionCacheManager;
import cn.nuonuoya.friend.converter.QuestionCaseConverter;
import cn.nuonuoya.friend.converter.QuestionConverter;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.QuestionQueryDTO;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.enums.UserQuestionStatusEnum;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.search.QuestionSemanticSearcher;
import cn.nuonuoya.friend.service.QuestionCaseService;
import cn.nuonuoya.friend.service.QuestionService;
import cn.nuonuoya.friend.service.TagService;
import cn.nuonuoya.friend.vo.QuestionPreNextVO;
import cn.nuonuoya.friend.vo.QuestionStatsVO;
import cn.nuonuoya.friend.vo.QuestionTagVO;
import cn.nuonuoya.friend.vo.QuestionVO;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// 题目业务服务实现类
@Slf4j
@Service
public class QuestionServiceImpl implements QuestionService {

    // 相似题推荐数量
    private static final int SIMILAR_LIMIT = 5;

    // 候选检索单次最多返回的数量
    private static final int CANDIDATE_LIMIT = 60;

    // 候选摘要的最大长度
    private static final int SUMMARY_LENGTH = 160;

    // 计算通过率所需的最少提交数（提交太少时通过率不可靠）
    private static final int MIN_SUBMITS_FOR_PASS_RATE = 5;

    // ES 题目文档字段：标题、描述、难度、标签、题目ID
    private static final String TITLE_FIELD = "title";
    private static final String CONTENT_FIELD = "content";
    private static final String DIFFICULTY_FIELD = "difficulty";
    private static final String TAG_IDS_FIELD = "tagIds";
    private static final String QUESTION_ID_FIELD = "questionId";

    // 注入ES持久层仓库
    @Autowired
    private QuestionRepository questionRepository;

    // 注入MySQL题目Mapper
    @Autowired
    private QuestionMapper questionMapper;

    // 注入用户代码提交记录Mapper
    @Autowired
    private UserSubmitMapper userSubmitMapper;

    // 注入ES操作模板
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    // 注入题目缓存管理器
    @Autowired
    private QuestionCacheManager questionCacheManager;

    @Autowired
    private QuestionCaseService questionCaseService;

    // 题目语义检索
    @Autowired
    private QuestionSemanticSearcher questionSemanticSearcher;

    // 题目标签查询
    @Autowired
    private TagService tagService;

    // 做题状态筛选范围：includeIds 为 null 表示不限定，excludeIds 为要排除的题目
    private record StatusScope(Set<Long> includeIds, Set<Long> excludeIds) {
    }

    // 分页全文检索题目列表（关键词、难度、标签、做题状态）
    @Override
    public TableDataResult<QuestionVO> search(QuestionQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new QuestionQueryDTO();
        }
        // 做题状态只对登录用户生效；限定集合为空时直接返回空页
        StatusScope scope = resolveStatusScope(queryDTO.getUserStatus());
        if (scope != null && scope.includeIds() != null && scope.includeIds().isEmpty()) {
            return TableDataResult.empty();
        }
        // 标签筛选：指定标签时按该标签，只指定分类时按该分类下任一标签；分类下没有标签时直接返回空页
        Set<Long> tagFilterIds = resolveTagFilterIds(queryDTO);
        if (tagFilterIds != null && tagFilterIds.isEmpty()) {
            return TableDataResult.empty();
        }

        try {
            // 初次若ES无数据则自动同步
            long esTotal = questionRepository.count();
            if (esTotal == 0) {
                syncAllQuestionsToEs();
            }

            int pageNum = Math.max(1, queryDTO.getPageNum());
            int pageSize = Math.max(1, queryDTO.getPageSize());
            boolean hasKeyword = StrUtil.isNotBlank(queryDTO.getKeyword());
            // 有关键字按相关度排序，无关键字与数据库降级查询保持一致（题目ID倒序）
            Pageable pageable = hasKeyword
                    ? PageRequest.of(pageNum - 1, pageSize)
                    : PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, QUESTION_ID_FIELD));
            Query keywordQuery = hasKeyword ? buildKeywordQuery(queryDTO.getKeyword().trim()) : Query.of(q -> q.matchAll(m -> m));
            List<Query> filters = buildFilters(queryDTO, tagFilterIds, scope);

            // 执行ES查询（不返回向量字段）
            NativeQuery esQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b.must(keywordQuery).filter(filters)))
                    .withPageable(pageable)
                    .withSourceFilter(new FetchSourceFilterBuilder().withExcludes(QuestionSemanticSearcher.EMBEDDING_FIELD).build())
                    .build();
            SearchHits<QuestionDoc> searchHits = elasticsearchOperations.search(esQuery, QuestionDoc.class);
            long total = searchHits.getTotalHits();
            List<QuestionDoc> docList = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            // 只按关键词检索且无结果时，首页用语义检索补充推荐（带标签或状态筛选时不推荐，避免结果不符合筛选条件）
            boolean onlyKeyword = tagFilterIds == null && scope == null;
            if (total == 0 && pageNum == 1 && hasKeyword && onlyKeyword) {
                List<QuestionDoc> semanticDocs = questionSemanticSearcher.search(
                        queryDTO.getKeyword(), queryDTO.getDifficulty(), pageSize);
                if (!semanticDocs.isEmpty()) {
                    List<QuestionVO> semanticList = QuestionConverter.toVOListFromDoc(semanticDocs);
                    semanticList.forEach(vo -> vo.setSemantic(true));
                    populateUserStatusAndTags(semanticList);
                    return TableDataResult.success(semanticList, semanticList.size());
                }
            }

            List<QuestionVO> voList = QuestionConverter.toVOListFromDoc(docList);
            // 批量装配用户做题状态与标签
            populateUserStatusAndTags(voList);
            return TableDataResult.success(voList, total);

        } catch (Exception e) {
            // ES异常时降级至MySQL查询保证可用性
            log.error("Elasticsearch题目检索异常，执行MySQL降级查询: {}", e.getMessage(), e);
            return searchFromMySQL(queryDTO, tagFilterIds, scope);
        }
    }

    // 查询题目详情
    @Override
    public QuestionVO getDetail(Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        QuestionVO vo = null;
        // 优先从ES读取
        try {
            Optional<QuestionDoc> docOpt = questionRepository.findById(questionId);
            if (docOpt.isPresent()) {
                vo = QuestionConverter.toVO(docOpt.get());
            }
        } catch (Exception e) {
            log.warn("从ES读取题目详情失败，降级查库: {}", e.getMessage());
        }

        // 兜底查MySQL
        if (vo == null) {
            TbQuestion entity = questionMapper.selectById(questionId);
            if (entity != null) {
                vo = QuestionConverter.toVO(entity);
            }
        }

        if (vo == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        populateUserStatusAndTags(Collections.singletonList(vo));
        vo.setSampleCases(QuestionCaseConverter.toSampleVOList(questionCaseService.listSamples(questionId)));
        return vo;
    }

    // 查询全部题目标签（题库筛选用）
    @Override
    public List<QuestionTagVO> listTags() {
        return tagService.listTags();
    }

    // 获取题库总题数与当前学员解题统计信息
    @Override
    public QuestionStatsVO getStats() {
        QuestionStatsVO statsVO = new QuestionStatsVO();
        long totalCount = questionMapper.selectCount(null);
        statsVO.setTotalCount(totalCount);

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            statsVO.setSolvedCount(0L);
            statsVO.setInProgressCount(0L);
            return statsVO;
        }

        Set<Long> solvedQuestionIds = new HashSet<>();
        Set<Long> attemptedQuestionIds = new HashSet<>();
        collectUserQuestionIds(userId, attemptedQuestionIds, solvedQuestionIds);
        statsVO.setSolvedCount((long) solvedQuestionIds.size());
        statsVO.setInProgressCount((long) (attemptedQuestionIds.size() - solvedQuestionIds.size()));
        return statsVO;
    }

    // 题目数据变更后刷新：清除题目顺序缓存并全量同步ES
    @Override
    public int refreshQuestionData() {
        questionCacheManager.evictListCache(null);
        return syncAllQuestionsToEs();
    }

    // AI 帮建竞赛的候选题目：混合检索后附上通过率（提交数不足时为空）
    @Override
    public List<FriendQuestionCandidateVO> listCandidates(FriendQuestionCandidateQueryDTO queryDTO) {
        int size = Math.max(1, Math.min(CANDIDATE_LIMIT, queryDTO.getSize() == null ? 1 : queryDTO.getSize()));
        List<QuestionDoc> docs = questionSemanticSearcher.candidates(queryDTO.getQuery(), queryDTO.getDifficulty(), size,
                CollUtil.emptyIfNull(queryDTO.getExcludeIds()));
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, int[]> submitStats = new HashMap<>();
        userSubmitMapper.selectList(new LambdaQueryWrapper<TbUserSubmit>()
                        .select(TbUserSubmit::getQuestionId, TbUserSubmit::getPass)
                        .in(TbUserSubmit::getQuestionId, docs.stream().map(QuestionDoc::getQuestionId).toList())
                        .ne(TbUserSubmit::getPass, SubmitPassEnum.JUDGING.getCode()))
                .forEach(submit -> {
                    int[] stat = submitStats.computeIfAbsent(submit.getQuestionId(), k -> new int[2]);
                    stat[0]++;
                    if (SubmitPassEnum.PASS.getCode().equals(submit.getPass())) {
                        stat[1]++;
                    }
                });
        List<FriendQuestionCandidateVO> result = new ArrayList<>(docs.size());
        for (QuestionDoc doc : docs) {
            FriendQuestionCandidateVO vo = new FriendQuestionCandidateVO();
            vo.setQuestionId(doc.getQuestionId());
            vo.setTitle(doc.getTitle());
            vo.setDifficulty(doc.getDifficulty());
            vo.setSummary(StrUtil.maxLength(StrUtil.nullToEmpty(doc.getContent()).replaceAll("\\s+", " "), SUMMARY_LENGTH));
            int[] stat = submitStats.get(doc.getQuestionId());
            vo.setPassRate(stat != null && stat[0] >= MIN_SUBMITS_FOR_PASS_RATE ? (double) stat[1] / stat[0] : null);
            result.add(vo);
        }
        return result;
    }

    // 相似题推荐：以题目向量做 kNN，排除当前题与当前用户已通过的题
    @Override
    public List<QuestionVO> listSimilar(Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        Long userId = SecurityUtils.getUserId();
        Set<Long> passedIds = new HashSet<>();
        if (userId != null) {
            userSubmitMapper.selectList(new LambdaQueryWrapper<TbUserSubmit>()
                            .select(TbUserSubmit::getQuestionId)
                            .eq(TbUserSubmit::getUserId, userId)
                            .eq(TbUserSubmit::getPass, SubmitPassEnum.PASS.getCode()))
                    .forEach(submit -> passedIds.add(submit.getQuestionId()));
        }
        try {
            List<QuestionVO> voList = QuestionConverter.toVOListFromDoc(
                    questionSemanticSearcher.similar(questionId, passedIds, SIMILAR_LIMIT));
            populateUserStatusAndTags(voList);
            return voList;
        } catch (Exception e) {
            log.warn("相似题检索失败, questionId = {}, error = {}", questionId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // 全量同步MySQL题目数据至ES索引（附带标签ID与题目向量），并移除MySQL中已不存在的文档
    private int syncAllQuestionsToEs() {
        List<TbQuestion> list = questionMapper.selectList(null);
        List<QuestionDoc> docList = QuestionConverter.toDocList(list);
        Map<Long, List<Long>> tagIdMap = tagService.mapQuestionTagIds(
                docList.stream().map(QuestionDoc::getQuestionId).toList());
        docList.forEach(doc -> doc.setTagIds(tagIdMap.getOrDefault(doc.getQuestionId(), Collections.emptyList())));
        questionSemanticSearcher.ensureMapping();
        questionSemanticSearcher.fillEmbeddings(docList);
        if (CollUtil.isNotEmpty(docList)) {
            questionRepository.saveAll(docList);
        }

        Set<Long> validIds = docList.stream().map(QuestionDoc::getQuestionId).collect(Collectors.toSet());
        List<Long> staleIds = new ArrayList<>();
        for (QuestionDoc doc : questionRepository.findAll()) {
            if (!validIds.contains(doc.getQuestionId())) {
                staleIds.add(doc.getQuestionId());
            }
        }
        if (!staleIds.isEmpty()) {
            questionRepository.deleteAllById(staleIds);
        }
        log.info("同步 {} 道题目至Elasticsearch索引，移除 {} 道已删除题目", docList.size(), staleIds.size());
        return docList.size();
    }

    // 关键词检索条件：标题或描述任一匹配
    private Query buildKeywordQuery(String keyword) {
        return Query.of(q -> q.bool(b -> b
                .should(s -> s.match(m -> m.field(TITLE_FIELD).query(keyword)))
                .should(s -> s.match(m -> m.field(CONTENT_FIELD).query(keyword)))
                .minimumShouldMatch("1")));
    }

    // 标签筛选范围：指定标签时只含该标签，只指定分类时为该分类下的全部标签，都未指定时为 null（不筛选）
    private Set<Long> resolveTagFilterIds(QuestionQueryDTO queryDTO) {
        if (queryDTO.getTagId() != null) {
            return Collections.singleton(queryDTO.getTagId());
        }
        if (queryDTO.getTagCategory() != null) {
            return tagService.listTagIdsByCategory(queryDTO.getTagCategory());
        }
        return null;
    }

    // 过滤条件：难度、标签、做题状态（不参与相关度打分）
    private List<Query> buildFilters(QuestionQueryDTO queryDTO, Set<Long> tagFilterIds, StatusScope scope) {
        List<Query> filters = new ArrayList<>();
        if (queryDTO.getDifficulty() != null && queryDTO.getDifficulty() > 0) {
            filters.add(Query.of(q -> q.term(t -> t.field(DIFFICULTY_FIELD).value(queryDTO.getDifficulty()))));
        }
        if (tagFilterIds != null) {
            List<FieldValue> tagValues = tagFilterIds.stream().map(FieldValue::of).toList();
            filters.add(Query.of(q -> q.terms(t -> t.field(TAG_IDS_FIELD).terms(v -> v.value(tagValues)))));
        }
        if (scope != null && scope.includeIds() != null) {
            List<String> includeIds = toIdValues(scope.includeIds());
            filters.add(Query.of(q -> q.ids(i -> i.values(includeIds))));
        }
        if (scope != null && CollUtil.isNotEmpty(scope.excludeIds())) {
            List<String> excludeIds = toIdValues(scope.excludeIds());
            filters.add(Query.of(q -> q.bool(b -> b.mustNot(m -> m.ids(i -> i.values(excludeIds))))));
        }
        return filters;
    }

    // 题目ID转为ES文档ID
    private List<String> toIdValues(Collection<Long> questionIds) {
        return questionIds.stream().map(String::valueOf).toList();
    }

    // 按做题状态得到题目范围：已攻克限定为通过的题，尝试中限定为提交过但未通过的题，未尝试排除提交过的题
    private StatusScope resolveStatusScope(Integer userStatus) {
        UserQuestionStatusEnum status = UserQuestionStatusEnum.getByCode(userStatus);
        Long userId = SecurityUtils.getUserId();
        if (status == null || userId == null) {
            return null;
        }
        Set<Long> attemptedIds = new HashSet<>();
        Set<Long> solvedIds = new HashSet<>();
        collectUserQuestionIds(userId, attemptedIds, solvedIds);
        return switch (status) {
            case SOLVED -> new StatusScope(solvedIds, null);
            case IN_PROGRESS -> {
                attemptedIds.removeAll(solvedIds);
                yield new StatusScope(attemptedIds, null);
            }
            case UNTOUCHED -> new StatusScope(null, attemptedIds);
        };
    }

    // 收集用户提交过的题目与已通过的题目
    private void collectUserQuestionIds(Long userId, Set<Long> attemptedIds, Set<Long> solvedIds) {
        List<TbUserSubmit> userSubmits = userSubmitMapper.selectList(
                new LambdaQueryWrapper<TbUserSubmit>()
                        .select(TbUserSubmit::getQuestionId, TbUserSubmit::getPass)
                        .eq(TbUserSubmit::getUserId, userId)
        );
        for (TbUserSubmit submit : userSubmits) {
            Long qId = submit.getQuestionId();
            if (qId == null) {
                continue;
            }
            attemptedIds.add(qId);
            if (SubmitPassEnum.PASS.getCode().equals(submit.getPass())) {
                solvedIds.add(qId);
            }
        }
    }

    // MySQL数据库降级分页检索（筛选条件与ES检索一致）
    private TableDataResult<QuestionVO> searchFromMySQL(QuestionQueryDTO queryDTO, Set<Long> tagFilterIds, StatusScope scope) {
        LambdaQueryWrapper<TbQuestion> lqw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword().trim();
            lqw.and(w -> w.like(TbQuestion::getTitle, kw).or().like(TbQuestion::getContent, kw));
        }
        if (queryDTO.getDifficulty() != null && queryDTO.getDifficulty() > 0) {
            lqw.eq(TbQuestion::getDifficulty, queryDTO.getDifficulty());
        }
        if (tagFilterIds != null) {
            Set<Long> taggedIds = tagService.listQuestionIdsByTags(tagFilterIds);
            if (taggedIds.isEmpty()) {
                return TableDataResult.empty();
            }
            lqw.in(TbQuestion::getQuestionId, taggedIds);
        }
        if (scope != null && scope.includeIds() != null) {
            lqw.in(TbQuestion::getQuestionId, scope.includeIds());
        }
        if (scope != null && CollUtil.isNotEmpty(scope.excludeIds())) {
            lqw.notIn(TbQuestion::getQuestionId, scope.excludeIds());
        }
        lqw.orderByDesc(TbQuestion::getQuestionId);

        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<TbQuestion> list = questionMapper.selectList(lqw);
        if (CollUtil.isEmpty(list)) {
            return TableDataResult.empty();
        }
        long total = new PageInfo<>(list).getTotal();
        List<QuestionVO> voList = list.stream()
                .map(QuestionConverter::toVO)
                .collect(Collectors.toList());
        // 批量装配用户做题状态与标签
        populateUserStatusAndTags(voList);
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

    // 批量装配题目标签与当前用户做题状态
    private void populateUserStatusAndTags(List<QuestionVO> voList) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        List<Long> questionIds = voList.stream()
                .map(QuestionVO::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<QuestionTagVO>> tagMap = tagService.mapQuestionTags(questionIds);

        Long userId = SecurityUtils.getUserId();
        Map<Long, Integer> statusMap = new HashMap<>();
        if (userId != null && CollUtil.isNotEmpty(questionIds)) {
            List<TbUserSubmit> submits = userSubmitMapper.selectList(
                    new LambdaQueryWrapper<TbUserSubmit>()
                            .select(TbUserSubmit::getQuestionId, TbUserSubmit::getPass)
                            .eq(TbUserSubmit::getUserId, userId)
                            .in(TbUserSubmit::getQuestionId, questionIds)
            );
            for (TbUserSubmit submit : submits) {
                Long qId = submit.getQuestionId();
                if (qId == null) {
                    continue;
                }
                Integer currentStatus = statusMap.get(qId);
                if (SubmitPassEnum.PASS.getCode().equals(submit.getPass())) {
                    statusMap.put(qId, UserQuestionStatusEnum.SOLVED.getCode());
                } else if (currentStatus == null || !UserQuestionStatusEnum.SOLVED.getCode().equals(currentStatus)) {
                    statusMap.put(qId, UserQuestionStatusEnum.IN_PROGRESS.getCode());
                }
            }
        }

        for (QuestionVO vo : voList) {
            vo.setUserStatus(statusMap.getOrDefault(vo.getQuestionId(), UserQuestionStatusEnum.UNTOUCHED.getCode()));
            vo.setTags(tagMap.getOrDefault(vo.getQuestionId(), Collections.emptyList()));
        }
    }
}
