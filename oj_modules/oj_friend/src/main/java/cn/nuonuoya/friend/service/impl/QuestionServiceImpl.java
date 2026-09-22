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
import cn.nuonuoya.friend.enums.QuestionDifficultyEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.enums.UserQuestionStatusEnum;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.search.QuestionSemanticSearcher;
import cn.nuonuoya.friend.service.QuestionCaseService;
import cn.nuonuoya.friend.service.QuestionService;
import cn.nuonuoya.friend.vo.QuestionPreNextVO;
import cn.nuonuoya.friend.vo.QuestionStatsVO;
import cn.nuonuoya.friend.vo.QuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
            // 有关键字按相关度排序，无关键字与数据库降级查询保持一致（题目ID倒序）
            Pageable pageable = StrUtil.isNotBlank(queryDTO.getKeyword())
                    ? PageRequest.of(pageNum - 1, pageSize)
                    : PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "questionId"));

            Query esQuery;
            if (criteria != null) {
                esQuery = new CriteriaQuery(criteria).setPageable(pageable);
            } else {
                esQuery = new CriteriaQuery(new Criteria()).setPageable(pageable);
            }

            // 执行ES查询（不返回向量字段）
            esQuery.addSourceFilter(new FetchSourceFilterBuilder().withExcludes(QuestionSemanticSearcher.EMBEDDING_FIELD).build());
            SearchHits<QuestionDoc> searchHits = elasticsearchOperations.search(esQuery, QuestionDoc.class);
            long total = searchHits.getTotalHits();
            List<QuestionDoc> docList = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            // 关键词无结果时，首页用语义检索补充推荐
            if (total == 0 && pageNum == 1 && StrUtil.isNotBlank(queryDTO.getKeyword())) {
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
            return searchFromMySQL(queryDTO);
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
        populateSingleUserStatusAndTags(vo);
        vo.setSampleCases(QuestionCaseConverter.toSampleVOList(questionCaseService.listSamples(questionId)));
        return vo;
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

        List<TbUserSubmit> userSubmits = userSubmitMapper.selectList(
                new LambdaQueryWrapper<TbUserSubmit>()
                        .select(TbUserSubmit::getQuestionId, TbUserSubmit::getPass)
                        .eq(TbUserSubmit::getUserId, userId)
        );

        Set<Long> solvedQuestionIds = new HashSet<>();
        Set<Long> attemptedQuestionIds = new HashSet<>();
        if (CollUtil.isNotEmpty(userSubmits)) {
            for (TbUserSubmit submit : userSubmits) {
                Long qId = submit.getQuestionId();
                if (qId == null) {
                    continue;
                }
                attemptedQuestionIds.add(qId);
                if (SubmitPassEnum.PASS.getCode().equals(submit.getPass())) {
                    solvedQuestionIds.add(qId);
                }
            }
        }
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

    // 全量同步MySQL题目数据至ES索引（附带题目向量），并移除MySQL中已不存在的文档
    private int syncAllQuestionsToEs() {
        List<TbQuestion> list = questionMapper.selectList(null);
        List<QuestionDoc> docList = QuestionConverter.toDocList(list);
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
        Long userId = SecurityUtils.getUserId();
        Map<Long, Integer> statusMap = new HashMap<>();
        if (userId != null) {
            List<Long> questionIds = voList.stream()
                    .map(QuestionVO::getQuestionId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(questionIds)) {
                List<TbUserSubmit> submits = userSubmitMapper.selectList(
                        new LambdaQueryWrapper<TbUserSubmit>()
                                .select(TbUserSubmit::getQuestionId, TbUserSubmit::getPass)
                                .eq(TbUserSubmit::getUserId, userId)
                                .in(TbUserSubmit::getQuestionId, questionIds)
                );
                if (CollUtil.isNotEmpty(submits)) {
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
            }
        }

        for (QuestionVO vo : voList) {
            Integer status = statusMap.getOrDefault(vo.getQuestionId(), UserQuestionStatusEnum.UNTOUCHED.getCode());
            vo.setUserStatus(status);
            vo.setTags(resolveQuestionTags(vo));
        }
    }

    // 单题装配题目标签与当前用户做题状态
    private void populateSingleUserStatusAndTags(QuestionVO vo) {
        if (vo == null) {
            return;
        }
        vo.setTags(resolveQuestionTags(vo));
        Long userId = SecurityUtils.getUserId();
        if (userId == null || vo.getQuestionId() == null) {
            vo.setUserStatus(UserQuestionStatusEnum.UNTOUCHED.getCode());
            return;
        }
        List<TbUserSubmit> submits = userSubmitMapper.selectList(
                new LambdaQueryWrapper<TbUserSubmit>()
                        .select(TbUserSubmit::getPass)
                        .eq(TbUserSubmit::getUserId, userId)
                        .eq(TbUserSubmit::getQuestionId, vo.getQuestionId())
        );
        Integer status = UserQuestionStatusEnum.UNTOUCHED.getCode();
        if (CollUtil.isNotEmpty(submits)) {
            boolean anyPass = submits.stream().anyMatch(s -> SubmitPassEnum.PASS.getCode().equals(s.getPass()));
            status = anyPass ? UserQuestionStatusEnum.SOLVED.getCode() : UserQuestionStatusEnum.IN_PROGRESS.getCode();
        }
        vo.setUserStatus(status);
    }

    // 启发式解析题目特征算法分类标签
    private List<String> resolveQuestionTags(QuestionVO vo) {
        if (vo == null) {
            return Collections.emptyList();
        }
        String title = StrUtil.nullToEmpty(vo.getTitle());
        String content = StrUtil.nullToEmpty(vo.getContent());
        String combined = title + " " + content;

        List<String> tags = new ArrayList<>();
        if (title.contains("两数之和") || (combined.contains("目标值") && combined.contains("数组下标"))) {
            tags.add("数组");
            tags.add("哈希表");
        } else if (title.contains("括号") || combined.contains("有效括号")) {
            tags.add("栈");
            tags.add("字符串");
        } else if (title.contains("回文") || combined.contains("回文数") || combined.contains("回文字符串")) {
            tags.add("数学");
            tags.add("双指针");
        } else {
            if (combined.contains("链表")) {
                tags.add("链表");
            }
            if (combined.contains("二叉树") || combined.contains("树节点")) {
                tags.add("树");
            }
            if (combined.contains("动态规划") || combined.contains("最优子结构")) {
                tags.add("动态规划");
            }
            if (combined.contains("二分查找") || combined.contains("有序数组")) {
                tags.add("二分查找");
            }
            if (combined.contains("贪心")) {
                tags.add("贪心");
            }
            if (combined.contains("图") || combined.contains("拓扑排序")) {
                tags.add("图论");
            }
        }

        if (tags.isEmpty()) {
            if (QuestionDifficultyEnum.EASY.getCode().equals(vo.getDifficulty())) {
                tags.add("基础算法");
            } else if (QuestionDifficultyEnum.MEDIUM.getCode().equals(vo.getDifficulty())) {
                tags.add("进阶算法");
            } else if (QuestionDifficultyEnum.HARD.getCode().equals(vo.getDifficulty())) {
                tags.add("高阶挑战");
            } else {
                tags.add("算法精选");
            }
        }
        return tags;
    }
}
