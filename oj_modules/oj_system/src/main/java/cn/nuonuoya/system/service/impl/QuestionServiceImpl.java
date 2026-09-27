package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.client.FriendQuestionClient;
import cn.nuonuoya.system.converter.QuestionConverter;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.domain.TbExamQuestion;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.dto.QuestionAddDTO;
import cn.nuonuoya.system.dto.QuestionCaseDTO;
import cn.nuonuoya.system.dto.QuestionDTO;
import cn.nuonuoya.system.dto.QuestionEditDTO;
import cn.nuonuoya.system.enums.QuestionCaseType;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.enums.QuestionPurpose;
import cn.nuonuoya.system.mapper.ExamMapper;
import cn.nuonuoya.system.mapper.ExamQuestionMapper;
import cn.nuonuoya.system.mapper.QuestionCaseMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.service.AppealService;
import cn.nuonuoya.system.service.QuestionEditorialService;
import cn.nuonuoya.system.service.QuestionService;
import cn.nuonuoya.system.service.TagService;
import cn.nuonuoya.mybatis.utils.TransactionUtils;
import cn.nuonuoya.system.vo.QuestionDetailVO;
import cn.nuonuoya.system.vo.QuestionTagVO;
import cn.nuonuoya.system.vo.QuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 题目业务实现类
@Slf4j
@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private FriendQuestionClient friendQuestionClient;

    @Autowired
    private QuestionCaseMapper questionCaseMapper;

    @Autowired
    private AppealService appealService;

    @Autowired
    private TagService tagService;

    @Autowired
    private QuestionEditorialService questionEditorialService;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    // 分页查询题目列表实现
    @Override
    public List<QuestionVO> list(QuestionDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new QuestionDTO();
        }
        // 开启 PageHelper 物理分页
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        // 执行联表查询，PageHelper 会自动拦截生成 COUNT 语句与物理 LIMIT 分页
        List<QuestionVO> list = questionMapper.selectQuestionList(queryDTO);
        if (CollUtil.isNotEmpty(list)) {
            // 补充难度描述文案，并批量装配当前页题目的标签
            List<Long> pageIds = list.stream().map(QuestionVO::getQuestionId).toList();
            Map<Long, List<QuestionTagVO>> tagMap = tagService.mapQuestionTags(pageIds);
            Set<Long> publishedIds = listPublishedQuestionIds(pageIds);
            Map<Long, Integer> upheldToFix = appealService.countUpheldToFix(pageIds);
            for (QuestionVO vo : list) {
                vo.setUpheldAppealCount(upheldToFix.getOrDefault(vo.getQuestionId(), 0));
                vo.setPublished(publishedIds.contains(vo.getQuestionId()));
                vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(vo.getDifficulty()));
                vo.setPurposeDesc(QuestionPurpose.getDescByValue(vo.getPurpose()));
                vo.setTags(tagMap.getOrDefault(vo.getQuestionId(), Collections.emptyList()));
            }
        }
        return list;
    }

    // 新增题目实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(QuestionAddDTO addDTO) {
        // 校验题目标题是否已存在
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<TbQuestion>()
                .eq(TbQuestion::getTitle, addDTO.getTitle().trim()));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_ALREADY_EXISTS);
        }
        checkCases(addDTO.getCases());
        TbQuestion question = QuestionConverter.toEntity(addDTO);
        int rows = questionMapper.insert(question);
        saveCases(question.getQuestionId(), addDTO.getCases());
        tagService.replaceQuestionTags(question.getQuestionId(), addDTO.getTagIds());
        questionEditorialService.save(question.getQuestionId(), addDTO.getEditorial());
        notifyQuestionChanged(rows);
        return rows;
    }

    // 查询题目详情实现
    @Override
    public QuestionDetailVO getDetail(Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 根据ID查库获取题目实体
        TbQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        QuestionDetailVO vo = QuestionConverter.toDetailVO(question);
        vo.setCases(QuestionConverter.toCaseVOList(questionCaseMapper.selectJudgeOrdered(questionId)));
        vo.setTags(tagService.listQuestionTags(questionId));
        vo.setEditorial(questionEditorialService.getContent(questionId));
        return vo;
    }

    // 修改题目实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int edit(QuestionEditDTO editDTO) {
        if (editDTO == null || editDTO.getQuestionId() == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验被修改题目是否存在
        TbQuestion oldQuestion = questionMapper.selectById(editDTO.getQuestionId());
        if (oldQuestion == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        // 校验题目标题是否与其他题目冲突（排除自身）
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<TbQuestion>()
                .eq(TbQuestion::getTitle, editDTO.getTitle().trim())
                .ne(TbQuestion::getQuestionId, editDTO.getQuestionId()));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_ALREADY_EXISTS);
        }
        checkCases(editDTO.getCases());
        checkPurposeChange(oldQuestion, editDTO.getPurpose());
        // 转换更新字段并入库；用例有变化时才整体替换（旧用例逻辑删除），用例ID保持稳定，
        // 提交记录里按用例ID保存的逐用例结果与「申诉成立、待修题」标记才不会因普通编辑失效
        TbQuestion question = QuestionConverter.toEntity(editDTO);
        int rows = questionMapper.updateById(question);
        if (casesChanged(editDTO.getQuestionId(), editDTO.getCases())) {
            questionCaseMapper.delete(new LambdaQueryWrapper<TbQuestionCase>()
                    .eq(TbQuestionCase::getQuestionId, editDTO.getQuestionId()));
            saveCases(editDTO.getQuestionId(), editDTO.getCases());
        }
        tagService.replaceQuestionTags(editDTO.getQuestionId(), editDTO.getTagIds());
        questionEditorialService.save(editDTO.getQuestionId(), editDTO.getEditorial());
        notifyQuestionChanged(rows);
        return rows;
    }

    // 删除题目实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验被删除题目是否存在
        TbQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        int rows = questionMapper.deleteById(questionId);
        questionCaseMapper.delete(new LambdaQueryWrapper<TbQuestionCase>()
                .eq(TbQuestionCase::getQuestionId, questionId));
        tagService.removeQuestionTags(questionId);
        questionEditorialService.remove(questionId);
        notifyQuestionChanged(rows);
        return rows;
    }

    // 已公开的题目：取这些题目所在的竞赛，至少有一场已结束的题即为已公开
    @Override
    public Set<Long> listPublishedQuestionIds(Collection<Long> questionIds) {
        if (CollUtil.isEmpty(questionIds)) {
            return Collections.emptySet();
        }
        List<TbExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<TbExamQuestion>()
                .select(TbExamQuestion::getExamId, TbExamQuestion::getQuestionId)
                .in(TbExamQuestion::getQuestionId, questionIds));
        if (relations.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> endedExamIds = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                        .select(TbExam::getExamId)
                        .in(TbExam::getExamId, relations.stream().map(TbExamQuestion::getExamId).collect(Collectors.toSet()))
                        .le(TbExam::getEndTime, LocalDateTime.now()))
                .stream()
                .map(TbExam::getExamId)
                .collect(Collectors.toSet());
        return relations.stream()
                .filter(relation -> endedExamIds.contains(relation.getExamId()))
                .map(TbExamQuestion::getQuestionId)
                .collect(Collectors.toSet());
    }

    // 公开已结束竞赛的题目：竞赛题里，被竞赛用过且这些竞赛全部结束的改为刷题；改完后通知 C 端刷新题库索引
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishFinishedContestQuestions() {
        List<Long> contestIds = questionMapper.selectList(new LambdaQueryWrapper<TbQuestion>()
                        .select(TbQuestion::getQuestionId)
                        .eq(TbQuestion::getPurpose, QuestionPurpose.CONTEST.getValue()))
                .stream()
                .map(TbQuestion::getQuestionId)
                .toList();
        if (contestIds.isEmpty()) {
            return 0;
        }
        List<TbExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<TbExamQuestion>()
                .select(TbExamQuestion::getExamId, TbExamQuestion::getQuestionId)
                .in(TbExamQuestion::getQuestionId, contestIds));
        if (relations.isEmpty()) {
            return 0;
        }
        Set<Long> examIds = relations.stream().map(TbExamQuestion::getExamId).collect(Collectors.toSet());
        Set<Long> unfinishedExamIds = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                        .select(TbExam::getExamId)
                        .in(TbExam::getExamId, examIds)
                        .gt(TbExam::getEndTime, LocalDateTime.now()))
                .stream()
                .map(TbExam::getExamId)
                .collect(Collectors.toSet());
        // 只要还有一场没结束就不公开（同一道题可能同时在多场未结束的竞赛里）
        Set<Long> blockedIds = relations.stream()
                .filter(relation -> unfinishedExamIds.contains(relation.getExamId()))
                .map(TbExamQuestion::getQuestionId)
                .collect(Collectors.toSet());
        List<Long> toPublish = relations.stream()
                .map(TbExamQuestion::getQuestionId)
                .distinct()
                .filter(questionId -> !blockedIds.contains(questionId))
                .toList();
        if (toPublish.isEmpty()) {
            return 0;
        }
        TbQuestion update = new TbQuestion();
        update.setPurpose(QuestionPurpose.PRACTICE.getValue());
        int rows = questionMapper.update(update, new LambdaQueryWrapper<TbQuestion>()
                .in(TbQuestion::getQuestionId, toPublish)
                .eq(TbQuestion::getPurpose, QuestionPurpose.CONTEST.getValue()));
        log.info("公开已结束竞赛的题目 {} 道", rows);
        notifyQuestionChanged(rows);
        return rows;
    }

    // 校验用途变更：刷题题不能改为竞赛题；竞赛题改为刷题时，它所在的竞赛必须都已结束（否则会提前出现在 C 端题库）
    private void checkPurposeChange(TbQuestion oldQuestion, Integer newPurpose) {
        if (Objects.equals(oldQuestion.getPurpose(), newPurpose)) {
            return;
        }
        if (Objects.equals(oldQuestion.getPurpose(), QuestionPurpose.PRACTICE.getValue())) {
            throw new ServiceException(ResultCode.FAILED_QUESTION_PURPOSE_LOCKED);
        }
        List<Long> examIds = examQuestionMapper.selectList(new LambdaQueryWrapper<TbExamQuestion>()
                        .select(TbExamQuestion::getExamId)
                        .eq(TbExamQuestion::getQuestionId, oldQuestion.getQuestionId()))
                .stream()
                .map(TbExamQuestion::getExamId)
                .distinct()
                .toList();
        if (examIds.isEmpty()) {
            return;
        }
        Long unfinished = examMapper.selectCount(new LambdaQueryWrapper<TbExam>()
                .in(TbExam::getExamId, examIds)
                .gt(TbExam::getEndTime, LocalDateTime.now()));
        if (unfinished != null && unfinished > 0) {
            throw new ServiceException(ResultCode.FAILED_QUESTION_IN_UNFINISHED_EXAM);
        }
    }

    // 校验用例：至少包含一个公开示例（用于题面展示与运行）
    private void checkCases(List<QuestionCaseDTO> cases) {
        boolean hasSample = cases.stream()
                .anyMatch(c -> Objects.equals(c.getIsSample(), QuestionCaseType.SAMPLE.getValue()));
        if (!hasSample) {
            throw new ServiceException(ResultCode.FAILED_QUESTION_NO_SAMPLE);
        }
    }

    // 批量写入题目用例
    private void saveCases(Long questionId, List<QuestionCaseDTO> cases) {
        for (TbQuestionCase entity : QuestionConverter.toCaseEntities(questionId, cases)) {
            questionCaseMapper.insert(entity);
        }
    }

    // 提交的用例与当前用例是否不同（按判题顺序逐项比较规范化后的内容）
    private boolean casesChanged(Long questionId, List<QuestionCaseDTO> cases) {
        List<TbQuestionCase> current = questionCaseMapper.selectJudgeOrdered(questionId);
        List<TbQuestionCase> incoming = QuestionConverter.toCaseEntities(questionId, cases);
        if (current.size() != incoming.size()) {
            return true;
        }
        for (int i = 0; i < current.size(); i++) {
            TbQuestionCase a = current.get(i);
            TbQuestionCase b = incoming.get(i);
            if (!Objects.equals(a.getDisplayInput(), b.getDisplayInput()) || !Objects.equals(a.getDisplayOutput(), b.getDisplayOutput())
                    || !Objects.equals(a.getJudgeInput(), b.getJudgeInput()) || !Objects.equals(a.getJudgeOutput(), b.getJudgeOutput())
                    || !Objects.equals(a.getIsSample(), b.getIsSample())) {
                return true;
            }
        }
        return false;
    }

    // 题目有变更时，事务提交后通知C端刷新题目缓存与ES索引
    private void notifyQuestionChanged(int rows) {
        if (rows <= 0) {
            return;
        }
        TransactionUtils.afterCommit(() -> {
            if (!friendQuestionClient.refreshQuestionData()) {
                log.warn("题目已保存但C端数据未刷新，C端题库可能暂未更新");
            }
        });
    }
}
