package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.nuonuoya.api.ai.dto.AiExamReviewDTO;
import cn.nuonuoya.api.ai.vo.AiExamReviewVO;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.client.AiReviewClient;
import cn.nuonuoya.friend.converter.ExamReviewConverter;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbExamReview;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.enums.ExamPublishStatusEnum;
import cn.nuonuoya.friend.enums.ExamRankSettledEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.ExamReviewMapper;
import cn.nuonuoya.friend.mapper.QuestionCaseMapper;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.ExamReviewService;
import cn.nuonuoya.friend.vo.ExamReviewQuestionVO;
import cn.nuonuoya.friend.vo.ExamReviewVO;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// 赛后复盘实现：数字由 SQL 统计，AI 只写点评；复盘按本人提交结果的摘要判断是否仍有效，失效时重新生成并原地覆盖；学员每场可手动重新生成 3 次
@Slf4j
@Service
public class ExamReviewServiceImpl implements ExamReviewService {

    // 每场竞赛手动重新生成的次数上限
    private static final int MAX_REGENERATE = 3;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private ExamReviewMapper examReviewMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionCaseMapper questionCaseMapper;

    @Autowired
    private AiReviewClient aiReviewClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // 读取仍有效的复盘
    @Override
    public ExamReviewVO getReview(Long examId) {
        Long userId = requireUserId();
        requireReviewableExam(examId, userId);
        String digest = digest(listSubmits(examId, userId));
        TbExamReview stored = selectStored(userId, examId);
        return stored != null && digest.equals(stored.getSourceDigest()) ? toStoredVO(stored) : null;
    }

    // 生成复盘：统计 → 挑出每题需要点评的提交 → AI 点评 → 保存
    @Override
    public ExamReviewVO generateReview(Long examId) {
        Long userId = requireUserId();
        TbExam exam = requireReviewableExam(examId, userId);
        List<TbUserSubmit> submits = listSubmits(examId, userId);
        String digest = digest(submits);
        TbExamReview stored = selectStored(userId, examId);
        if (stored != null && digest.equals(stored.getSourceDigest())) {
            return toStoredVO(stored);
        }
        ExamReviewVO vo = buildReview(exam, userId, submits);
        save(userId, examId, vo, digest);
        vo.setRegenerateRemaining(MAX_REGENERATE - (stored == null ? 0 : stored.getRegenerateCount()));
        return vo;
    }

    // 手动重新生成：先查次数，生成后按「次数未满」条件覆盖并计数，并发下也不会超过上限
    @Override
    public ExamReviewVO regenerateReview(Long examId) {
        Long userId = requireUserId();
        TbExam exam = requireReviewableExam(examId, userId);
        TbExamReview stored = selectStored(userId, examId);
        if (stored == null) {
            return generateReview(examId);
        }
        if (stored.getRegenerateCount() >= MAX_REGENERATE) {
            throw new ServiceException(ResultCode.FAILED_EXAM_REVIEW_REGENERATE_LIMIT);
        }
        List<TbUserSubmit> submits = listSubmits(examId, userId);
        ExamReviewVO vo = buildReview(exam, userId, submits);
        String content = ExamReviewConverter.toContent(vo);
        String digest = digest(submits);
        Integer updated = transactionTemplate.execute(status -> examReviewMapper.update(null, new LambdaUpdateWrapper<TbExamReview>()
                .set(TbExamReview::getContent, content)
                .set(TbExamReview::getSourceDigest, digest)
                .set(TbExamReview::getUpdateTime, LocalDateTime.now())
                .setSql("regenerate_count = regenerate_count + 1")
                .eq(TbExamReview::getReviewId, stored.getReviewId())
                .lt(TbExamReview::getRegenerateCount, MAX_REGENERATE)));
        if (updated == null || updated == 0) {
            throw new ServiceException(ResultCode.FAILED_EXAM_REVIEW_REGENERATE_LIMIT);
        }
        vo.setRegenerateRemaining(MAX_REGENERATE - stored.getRegenerateCount() - 1);
        return vo;
    }

    // 统计成绩与逐题情况，挑出每题需要点评的提交交给 AI，组装成复盘
    private ExamReviewVO buildReview(TbExam exam, Long userId, List<TbUserSubmit> submits) {
        Long examId = exam.getExamId();
        TbUserExam userExam = userExamMapper.selectOne(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getUserId, userId)
                .eq(TbUserExam::getExamId, examId));
        int participantCount = Math.toIntExact(userExamMapper.selectCount(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getExamId, examId)));
        ExamReviewVO vo = ExamReviewConverter.toReviewVO(exam, userExam, participantCount,
                examReviewMapper.selectQuestionStats(examId, userId, SubmitPassEnum.PASS.getCode()));

        AiExamReviewDTO request = ExamReviewConverter.toRequest(vo);
        Map<Long, List<TbUserSubmit>> submitsByQuestion = submits.stream()
                .collect(Collectors.groupingBy(TbUserSubmit::getQuestionId));
        Map<Long, TbQuestion> questionById = questionMapper.selectByIds(vo.getQuestions().stream()
                        .map(ExamReviewQuestionVO::getQuestionId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(TbQuestion::getQuestionId, Function.identity()));
        for (int i = 0; i < vo.getQuestions().size(); i++) {
            ExamReviewQuestionVO stat = vo.getQuestions().get(i);
            TbUserSubmit focus = focusSubmit(submitsByQuestion.getOrDefault(stat.getQuestionId(), Collections.emptyList()));
            TbQuestionCase failCase = focus == null || focus.getFailCaseId() == null ? null : questionCaseMapper.selectById(focus.getFailCaseId());
            request.getQuestions().add(ExamReviewConverter.toQuestionRequest(i + 1, stat,
                    questionById.get(stat.getQuestionId()), focus, failCase));
        }
        AiExamReviewVO result = aiReviewClient.review(request);

        ExamReviewConverter.fillComments(vo, result);
        vo.setGeneratedTime(LocalDateTime.now());
        return vo;
    }

    // 读出已存的复盘并补上剩余重新生成次数（次数以表里为准，不用内容里的快照）
    private ExamReviewVO toStoredVO(TbExamReview stored) {
        ExamReviewVO vo = ExamReviewConverter.fromContent(stored.getContent());
        vo.setRegenerateRemaining(MAX_REGENERATE - stored.getRegenerateCount());
        return vo;
    }

    // 已结束且已结算、本人有提交的竞赛
    @Override
    public Set<Long> listReviewableExamIds(Long userId, Collection<Long> examIds) {
        if (userId == null || CollUtil.isEmpty(examIds)) {
            return Collections.emptySet();
        }
        List<Long> settledIds = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                        .select(TbExam::getExamId)
                        .in(TbExam::getExamId, examIds)
                        .eq(TbExam::getRankSettled, ExamRankSettledEnum.SETTLED.getCode())
                        .le(TbExam::getEndTime, LocalDateTime.now()))
                .stream()
                .map(TbExam::getExamId)
                .toList();
        if (settledIds.isEmpty()) {
            return Collections.emptySet();
        }
        return userSubmitMapper.selectList(new LambdaQueryWrapper<TbUserSubmit>()
                        .select(TbUserSubmit::getExamId)
                        .eq(TbUserSubmit::getUserId, userId)
                        .in(TbUserSubmit::getExamId, settledIds)
                        .groupBy(TbUserSubmit::getExamId))
                .stream()
                .map(TbUserSubmit::getExamId)
                .collect(Collectors.toSet());
    }

    // 校验可以复盘：竞赛已发布、本人已报名、已结束且已结算（有无提交由调用方在取提交后判断）
    private TbExam requireReviewableExam(Long examId, Long userId) {
        if (examId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        TbExam exam = examMapper.selectById(examId);
        if (exam == null || !ExamPublishStatusEnum.PUBLISHED.getCode().equals(exam.getStatus())) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        boolean enrolled = userExamMapper.exists(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getUserId, userId)
                .eq(TbUserExam::getExamId, examId));
        if (!enrolled) {
            throw new ServiceException(ResultCode.FAILED_EXAM_NOT_ENROLLED);
        }
        if (exam.getEndTime() == null || LocalDateTime.now().isBefore(exam.getEndTime())
                || !ExamRankSettledEnum.SETTLED.getCode().equals(exam.getRankSettled())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_REVIEW_NOT_READY);
        }
        return exam;
    }

    // 本人这场的全部提交（按提交时间升序），没有提交时不能复盘
    private List<TbUserSubmit> listSubmits(Long examId, Long userId) {
        List<TbUserSubmit> submits = userSubmitMapper.selectList(new LambdaQueryWrapper<TbUserSubmit>()
                .select(TbUserSubmit::getSubmitId, TbUserSubmit::getQuestionId, TbUserSubmit::getUserCode,
                        TbUserSubmit::getPass, TbUserSubmit::getJudgeStatus, TbUserSubmit::getExeMessage,
                        TbUserSubmit::getPassCount, TbUserSubmit::getTotalCount, TbUserSubmit::getFailCaseId,
                        TbUserSubmit::getCreateTime)
                .eq(TbUserSubmit::getExamId, examId)
                .eq(TbUserSubmit::getUserId, userId)
                .orderByAsc(TbUserSubmit::getCreateTime, TbUserSubmit::getSubmitId));
        if (submits.isEmpty()) {
            throw new ServiceException(ResultCode.FAILED_EXAM_REVIEW_NO_SUBMIT);
        }
        return submits;
    }

    // 提交结果摘要：每条提交的 ID、是否通过与判题结论，申诉改判或重判后会变
    private String digest(List<TbUserSubmit> submits) {
        return DigestUtil.md5Hex(submits.stream()
                .sorted(Comparator.comparing(TbUserSubmit::getSubmitId))
                .map(s -> s.getSubmitId() + ":" + s.getPass() + ":" + s.getJudgeStatus())
                .collect(Collectors.joining(";")));
    }

    // 一道题需要点评的提交：未通过取最后一次，通过但错过取通过前最后一次失败，一次通过或没有提交为空
    private TbUserSubmit focusSubmit(List<TbUserSubmit> questionSubmits) {
        if (questionSubmits.isEmpty()) {
            return null;
        }
        for (int i = 0; i < questionSubmits.size(); i++) {
            if (SubmitPassEnum.PASS.getCode().equals(questionSubmits.get(i).getPass())) {
                return i == 0 ? null : questionSubmits.get(i - 1);
            }
        }
        return questionSubmits.get(questionSubmits.size() - 1);
    }

    // 已存的复盘
    private TbExamReview selectStored(Long userId, Long examId) {
        return examReviewMapper.selectOne(new LambdaQueryWrapper<TbExamReview>()
                .eq(TbExamReview::getUserId, userId)
                .eq(TbExamReview::getExamId, examId));
    }

    // 保存复盘：没有就新增，有就原地覆盖（不改重新生成次数）；并发生成撞上唯一约束时以先写入的为准
    private void save(Long userId, Long examId, ExamReviewVO vo, String digest) {
        String content = ExamReviewConverter.toContent(vo);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                int updated = examReviewMapper.update(null, new LambdaUpdateWrapper<TbExamReview>()
                        .set(TbExamReview::getContent, content)
                        .set(TbExamReview::getSourceDigest, digest)
                        .set(TbExamReview::getUpdateTime, LocalDateTime.now())
                        .eq(TbExamReview::getUserId, userId)
                        .eq(TbExamReview::getExamId, examId));
                if (updated == 0) {
                    TbExamReview review = new TbExamReview();
                    review.setUserId(userId);
                    review.setExamId(examId);
                    review.setContent(content);
                    review.setSourceDigest(digest);
                    examReviewMapper.insert(review);
                }
            });
        } catch (DuplicateKeyException e) {
            log.info("赛后复盘已由并发请求写入, userId = {}, examId = {}", userId, examId);
        }
    }

    // 从认证上下文取当前用户
    private Long requireUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        return userId;
    }
}
