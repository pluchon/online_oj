package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.nuonuoya.api.ai.dto.AiAppealCaseDTO;
import cn.nuonuoya.api.ai.dto.AiAppealReviewDTO;
import cn.nuonuoya.api.ai.vo.AiAppealReviewVO;
import cn.nuonuoya.api.friend.dto.FriendAppealHandleDTO;
import cn.nuonuoya.api.friend.dto.FriendAppealQueryDTO;
import cn.nuonuoya.api.friend.enums.AppealStatusEnum;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendAppealQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendAppealVO;
import cn.nuonuoya.api.friend.vo.FriendCaseResultVO;
import cn.nuonuoya.api.friend.vo.FriendPageVO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.api.judge.enums.QuestionDifficultyScoreEnum;
import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.cache.DailyQuotaManager;
import cn.nuonuoya.friend.client.AiAppealClient;
import cn.nuonuoya.friend.client.AiModerationClient;
import cn.nuonuoya.friend.constants.FriendCacheConstants;
import cn.nuonuoya.friend.converter.AppealConverter;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.domain.TbQuestionEditorial;
import cn.nuonuoya.friend.domain.TbSubmitAppeal;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.AppealCreateDTO;
import cn.nuonuoya.friend.enums.MessageTypeEnum;
import cn.nuonuoya.friend.enums.QuestionCaseTypeEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.QuestionEditorialMapper;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.SubmitAppealMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.AppealService;
import cn.nuonuoya.friend.service.MessageService;
import cn.nuonuoya.friend.service.QuestionCaseService;
import cn.nuonuoya.friend.vo.AppealQuotaVO;
import cn.nuonuoya.friend.vo.AppealReviewVO;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;
import cn.nuonuoya.mybatis.utils.TransactionUtils;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.utils.SecurityUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

// 提交申诉业务实现（D-017）：AI 初审放行后才能正式申诉；学员看到的文案由后端按结论选定，AI 分析只给管理员
@Slf4j
@Service
public class AppealServiceImpl implements AppealService {

    // 初审时最多带给 AI 的未通过用例数
    private static final int FAILED_CASE_LIMIT = 3;

    // 初审放行时给学员的说明
    private static final String REVIEW_ALLOWED_MESSAGE = "AI 初审认为这次判题可能有误，可以提交正式申诉。请写清你认为判错的理由，管理员核实后会通过站内消息告诉你结果。";

    // 初审未放行时给学员的说明（不透露任何用例内容）
    private static final String REVIEW_DENIED_MESSAGE = "AI 初审没有发现判题问题，这次未通过更可能是代码本身的原因。建议再检查边界情况、数据范围和输出格式，也可以用 AI 辅导分析这次提交。";

    // 站内消息标题
    private static final String NOTICE_TITLE = "申诉结果通知";

    // 消息里的提交时间格式
    private static final String NOTICE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    // AI 初审每人每日次数，可在 Nacos 中覆盖
    @Value("${oj.appeal.review-daily-limit:10}")
    private int reviewDailyLimit;

    // 正式申诉每人每日次数，可在 Nacos 中覆盖
    @Value("${oj.appeal.daily-limit:5}")
    private int appealDailyLimit;

    @Autowired
    private SubmitAppealMapper submitAppealMapper;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionEditorialMapper questionEditorialMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionCaseService questionCaseService;

    @Autowired
    private AiAppealClient aiAppealClient;

    @Autowired
    private AiModerationClient aiModerationClient;

    @Autowired
    private DailyQuotaManager dailyQuotaManager;

    @Autowired
    private RedisService redisService;

    @Autowired
    private MessageService messageService;

    // 当前用户今日的初审与申诉剩余次数
    @Override
    public AppealQuotaVO getQuota() {
        return quota(requireUserId());
    }

    // AI 初审：同一条提交在结论有效期内不重复扣次数；AI 不可用时归还次数
    @Override
    public AppealReviewVO review(Long submitId) {
        Long userId = requireUserId();
        TbUserSubmit submit = requireAppealable(submitId, userId);
        AiAppealReviewVO result = cachedReview(submitId);
        if (result == null) {
            if (!dailyQuotaManager.tryAcquire(FriendCacheConstants.APPEAL_REVIEW_QUOTA_KEY, reviewDailyLimit, userId)) {
                throw new ServiceException(ResultCode.FAILED_APPEAL_REVIEW_QUOTA);
            }
            result = aiAppealClient.review(buildReviewRequest(submit));
            if (result == null) {
                dailyQuotaManager.release(FriendCacheConstants.APPEAL_REVIEW_QUOTA_KEY, userId);
                throw new ServiceException(ResultCode.FAILED_AI_BUSY);
            }
            redisService.setCacheObject(FriendCacheConstants.APPEAL_REVIEW_RESULT_KEY + submitId, JSON.toJSONString(result),
                    FriendCacheConstants.APPEAL_REVIEW_RESULT_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("申诉初审完成, submitId = {}, suspicious = {}", submitId, result.getSuspicious());
        }
        AppealReviewVO vo = new AppealReviewVO();
        vo.setAllowed(Boolean.TRUE.equals(result.getSuspicious()));
        vo.setMessage(vo.getAllowed() ? REVIEW_ALLOWED_MESSAGE : REVIEW_DENIED_MESSAGE);
        vo.setQuota(quota(userId));
        return vo;
    }

    // 正式申诉：须有放行的初审结论；理由过内容审核；每条提交只能申诉一次（唯一约束兜底并发）
    @Override
    public void create(AppealCreateDTO createDTO) {
        Long userId = requireUserId();
        TbUserSubmit submit = requireAppealable(createDTO.getSubmitId(), userId);
        AiAppealReviewVO review = cachedReview(submit.getSubmitId());
        if (review == null || !Boolean.TRUE.equals(review.getSuspicious())) {
            throw new ServiceException(ResultCode.FAILED_APPEAL_NOT_REVIEWED);
        }
        String reason = createDTO.getReason().trim();
        aiModerationClient.checkTexts(List.of(reason), "申诉理由");
        if (!dailyQuotaManager.tryAcquire(FriendCacheConstants.APPEAL_QUOTA_KEY, appealDailyLimit, userId)) {
            throw new ServiceException(ResultCode.FAILED_APPEAL_QUOTA);
        }

        TbSubmitAppeal appeal = new TbSubmitAppeal();
        appeal.setSubmitId(submit.getSubmitId());
        appeal.setUserId(userId);
        appeal.setQuestionId(submit.getQuestionId());
        appeal.setExamId(submit.getExamId());
        appeal.setReason(reason);
        appeal.setAiAnalysis(Objects.toString(review.getAnalysis(), ""));
        appeal.setOriginJudgeStatus(submit.getJudgeStatus());
        appeal.setStatus(AppealStatusEnum.PENDING.getCode());
        appeal.setCreateBy(userId);
        appeal.setCreateTime(LocalDateTime.now());
        try {
            submitAppealMapper.insert(appeal);
        } catch (DuplicateKeyException e) {
            dailyQuotaManager.release(FriendCacheConstants.APPEAL_QUOTA_KEY, userId);
            throw new ServiceException(ResultCode.FAILED_APPEAL_EXISTS);
        } catch (RuntimeException e) {
            dailyQuotaManager.release(FriendCacheConstants.APPEAL_QUOTA_KEY, userId);
            throw e;
        }
        redisService.deleteObject(FriendCacheConstants.APPEAL_REVIEW_RESULT_KEY + submit.getSubmitId());
        log.info("收到申诉, appealId = {}, submitId = {}", appeal.getAppealId(), submit.getSubmitId());
    }

    // 给本题提交记录补申诉状态与是否可申诉（批量查询，避免逐条查库）
    @Override
    public void fillHistoryAppeal(List<SubmitHistoryVO> voList, List<TbUserSubmit> submits) {
        if (CollUtil.isEmpty(submits)) {
            return;
        }
        Map<Long, Integer> statusBySubmit = submitAppealMapper.selectList(new LambdaQueryWrapper<TbSubmitAppeal>()
                        .select(TbSubmitAppeal::getSubmitId, TbSubmitAppeal::getStatus)
                        .in(TbSubmitAppeal::getSubmitId, submits.stream().map(TbUserSubmit::getSubmitId).toList()))
                .stream()
                .collect(Collectors.toMap(TbSubmitAppeal::getSubmitId, TbSubmitAppeal::getStatus, (a, b) -> a));
        Map<Long, TbExam> examById = examsOf(submits);
        Map<Long, TbUserSubmit> submitById = submits.stream().collect(Collectors.toMap(TbUserSubmit::getSubmitId, Function.identity()));
        for (SubmitHistoryVO vo : voList) {
            Integer appealStatus = statusBySubmit.get(vo.getSubmitId());
            TbUserSubmit submit = submitById.get(vo.getSubmitId());
            vo.setAppealStatus(appealStatus);
            vo.setAppealable(appealStatus == null && submit != null
                    && notAppealableReason(submit, submit.getExamId() == null ? null : examById.get(submit.getExamId())) == null);
        }
    }

    // 管理端分页查询申诉（按申诉时间倒序；题目条件为空列表时直接返回空页）
    @Override
    public FriendPageVO<FriendAppealVO> listForManage(FriendAppealQueryDTO queryDTO) {
        FriendPageVO<FriendAppealVO> page = new FriendPageVO<>();
        page.setRows(Collections.emptyList());
        if (queryDTO == null) {
            queryDTO = new FriendAppealQueryDTO();
        }
        if (queryDTO.getQuestionIds() != null && queryDTO.getQuestionIds().isEmpty()) {
            return page;
        }
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(queryDTO.getPageNum());
        pageQuery.setPageSize(queryDTO.getPageSize());
        PageHelper.startPage(pageQuery.getPageNum(), pageQuery.getPageSize());
        List<TbSubmitAppeal> appeals = submitAppealMapper.selectList(new LambdaQueryWrapper<TbSubmitAppeal>()
                .eq(queryDTO.getUserId() != null, TbSubmitAppeal::getUserId, queryDTO.getUserId())
                .in(CollUtil.isNotEmpty(queryDTO.getQuestionIds()), TbSubmitAppeal::getQuestionId, queryDTO.getQuestionIds())
                .ge(queryDTO.getStartTime() != null, TbSubmitAppeal::getCreateTime, queryDTO.getStartTime())
                .orderByDesc(TbSubmitAppeal::getCreateTime)
                .orderByDesc(TbSubmitAppeal::getAppealId));
        if (appeals.isEmpty()) {
            return page;
        }
        page.setTotal(new PageInfo<>(appeals).getTotal());
        page.setRows(BeanUtil.copyToList(appeals, FriendAppealVO.class));
        return page;
    }

    // 管理端查询申诉详情（含被申诉提交的代码与逐用例结果）
    @Override
    public FriendAppealDetailVO getForManage(Long appealId) {
        TbSubmitAppeal appeal = appealId == null ? null : submitAppealMapper.selectById(appealId);
        if (appeal == null) {
            return null;
        }
        return AppealConverter.toDetailVO(appeal, userSubmitMapper.selectById(appeal.getSubmitId()));
    }

    // 裁定（D-018：已裁定的也可以改）：以读到的原状态做条件更新，防止两位管理员同时改；
    // 提交结论跟随申诉状态——改为通过即改判为满分，从通过改走则恢复申诉时的原结论；结论变化或给出终态时通知学员
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(Long appealId, FriendAppealHandleDTO handleDTO) {
        AppealStatusEnum target = AppealStatusEnum.getByCode(handleDTO.getStatus());
        if (appealId == null || target == null || target == AppealStatusEnum.PENDING) {
            return false;
        }
        TbSubmitAppeal appeal = submitAppealMapper.selectById(appealId);
        if (appeal == null) {
            return false;
        }
        AppealStatusEnum origin = AppealStatusEnum.getByCode(appeal.getStatus());
        if (origin == target) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = submitAppealMapper.update(null, new LambdaUpdateWrapper<TbSubmitAppeal>()
                .set(TbSubmitAppeal::getStatus, target.getCode())
                .set(TbSubmitAppeal::getHandleBy, handleDTO.getHandlerId())
                .set(TbSubmitAppeal::getHandleTime, now)
                .set(TbSubmitAppeal::getUpdateBy, handleDTO.getHandlerId())
                .set(TbSubmitAppeal::getUpdateTime, now)
                .eq(TbSubmitAppeal::getAppealId, appealId)
                .eq(TbSubmitAppeal::getStatus, appeal.getStatus()));
        if (rows == 0) {
            return false;
        }
        boolean wasUpheld = origin == AppealStatusEnum.UPHELD;
        boolean nowUpheld = target == AppealStatusEnum.UPHELD;
        TbQuestion question = questionMapper.selectById(appeal.getQuestionId());
        if (wasUpheld != nowUpheld) {
            applyVerdict(appeal, question, nowUpheld);
        }
        if (target.isFinal() || (origin != null && origin.isFinal())) {
            notifyResult(appeal, question, target, origin != null && origin.isFinal());
        }
        log.info("申诉已裁定, appealId = {}, {} -> {}, handler = {}", appealId, origin, target, handleDTO.getHandlerId());
        return true;
    }

    // 通知学员裁定结果（改判过的在开头说明结果已更新）
    private void notifyResult(TbSubmitAppeal appeal, TbQuestion question, AppealStatusEnum target, boolean updated) {
        TbUserSubmit submit = userSubmitMapper.selectById(appeal.getSubmitId());
        String title = question == null ? "已删除的题目" : question.getTitle();
        String submitTime = submit == null ? "" : LocalDateTimeUtil.format(submit.getCreateTime(), NOTICE_TIME_PATTERN) + " ";
        String prefix = updated ? "申诉结果已更新：" : "";
        String content = switch (target) {
            case UPHELD -> prefix + "你对「" + title + "」的申诉已通过，" + submitTime + "的这次提交已改判为通过。感谢反馈，我们会尽快修正这道题的测试用例。";
            case REJECTED -> prefix + "你对「" + title + "」的申诉未通过，经管理员核查，" + submitTime + "这次提交的判题结果无误。";
            default -> prefix + "你对「" + title + "」的申诉需要进一步核实，" + submitTime + "这次提交暂时恢复为原判题结果。";
        };
        messageService.sendSystemMessage(appeal.getUserId(), MessageTypeEnum.AUDIT, NOTICE_TITLE, content);
    }

    // 指定题目中裁定为通过的申诉数与最近裁定时间
    @Override
    public List<FriendAppealQuestionStatVO> upheldStats(List<Long> questionIds) {
        if (CollUtil.isEmpty(questionIds)) {
            return Collections.emptyList();
        }
        Map<Long, List<TbSubmitAppeal>> byQuestion = submitAppealMapper.selectList(new LambdaQueryWrapper<TbSubmitAppeal>()
                        .select(TbSubmitAppeal::getQuestionId, TbSubmitAppeal::getHandleTime)
                        .eq(TbSubmitAppeal::getStatus, AppealStatusEnum.UPHELD.getCode())
                        .in(TbSubmitAppeal::getQuestionId, questionIds))
                .stream()
                .collect(Collectors.groupingBy(TbSubmitAppeal::getQuestionId));
        List<FriendAppealQuestionStatVO> result = new ArrayList<>(byQuestion.size());
        byQuestion.forEach((questionId, appeals) -> {
            FriendAppealQuestionStatVO stat = new FriendAppealQuestionStatVO();
            stat.setQuestionId(questionId);
            stat.setUpheldCount(appeals.size());
            stat.setLatestHandleTime(appeals.stream().map(TbSubmitAppeal::getHandleTime)
                    .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
            result.add(stat);
        });
        return result;
    }

    // 提交结论跟随申诉：通过时改判为运行通过并按难度记满分；撤回通过时恢复申诉时的原结论，得分按判题公式由通过用例数重算；
    // 所在竞赛的排名缓存事务提交后失效（已结算竞赛的名次与战报不变）
    private void applyVerdict(TbSubmitAppeal appeal, TbQuestion question, boolean pass) {
        TbUserSubmit submit = userSubmitMapper.selectById(appeal.getSubmitId());
        if (submit == null) {
            return;
        }
        int fullScore = QuestionDifficultyScoreEnum.getFullScore(question == null ? null : question.getDifficulty());
        int total = submit.getTotalCount() == null ? 0 : submit.getTotalCount();
        int passCount = submit.getPassCount() == null ? 0 : submit.getPassCount();
        int originScore = total <= 0 ? 0 : (int) Math.round((double) fullScore * passCount / total);
        userSubmitMapper.update(null, new LambdaUpdateWrapper<TbUserSubmit>()
                .set(TbUserSubmit::getPass, pass ? SubmitPassEnum.PASS.getCode() : SubmitPassEnum.NOT_PASS.getCode())
                .set(TbUserSubmit::getJudgeStatus, pass ? JudgeStatusEnum.AC.getCode() : appeal.getOriginJudgeStatus())
                .set(TbUserSubmit::getScore, pass ? fullScore : originScore)
                .set(TbUserSubmit::getUpdateTime, LocalDateTime.now())
                .eq(TbUserSubmit::getSubmitId, appeal.getSubmitId()));
        if (appeal.getExamId() != null) {
            TransactionUtils.afterCommit(() -> redisService.deleteObject(FriendCacheConstants.EXAM_RANK_LIST_KEY + appeal.getExamId()));
        }
    }

    // 读取初审结论缓存，没有时返回 null
    private AiAppealReviewVO cachedReview(Long submitId) {
        String json = redisService.getCacheObject(FriendCacheConstants.APPEAL_REVIEW_RESULT_KEY + submitId, String.class);
        return StringUtils.hasText(json) ? JSON.parseObject(json, AiAppealReviewVO.class) : null;
    }

    // 取出当前用户可以申诉的提交，不满足条件时抛出对应提示
    private TbUserSubmit requireAppealable(Long submitId, Long userId) {
        TbUserSubmit submit = submitId == null ? null : userSubmitMapper.selectById(submitId);
        if (submit == null || !userId.equals(submit.getUserId())) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        Long appealed = submitAppealMapper.selectCount(new LambdaQueryWrapper<TbSubmitAppeal>()
                .eq(TbSubmitAppeal::getSubmitId, submitId));
        if (appealed != null && appealed > 0) {
            throw new ServiceException(ResultCode.FAILED_APPEAL_EXISTS);
        }
        String reason = notAppealableReason(submit, submit.getExamId() == null ? null : examMapper.selectById(submit.getExamId()));
        if (reason != null) {
            throw new ServiceException(ResultCode.FAILED_APPEAL_NOT_ALLOWED, reason);
        }
        return submit;
    }

    // 不能申诉的原因（可以申诉时返回 null）：只有已出结论的未通过提交可以申诉，系统错误请重新提交，竞赛提交要等竞赛结束
    private String notAppealableReason(TbUserSubmit submit, TbExam exam) {
        if (!SubmitPassEnum.NOT_PASS.getCode().equals(submit.getPass())) {
            return "只有未通过的提交可以申诉";
        }
        if (JudgeStatusEnum.SE.getCode().equals(submit.getJudgeStatus())) {
            return "系统错误导致的未通过请直接重新提交";
        }
        if (submit.getExamId() != null && (exam == null || exam.getEndTime() == null || LocalDateTime.now().isBefore(exam.getEndTime()))) {
            return "竞赛结束后才能申诉";
        }
        return null;
    }

    // 批量读取提交所属的竞赛
    private Map<Long, TbExam> examsOf(List<TbUserSubmit> submits) {
        Set<Long> examIds = submits.stream().map(TbUserSubmit::getExamId).filter(Objects::nonNull).collect(Collectors.toSet());
        return examIds.isEmpty() ? Collections.emptyMap()
                : examMapper.selectByIds(examIds).stream().collect(Collectors.toMap(TbExam::getExamId, Function.identity()));
    }

    // 组装 AI 初审请求：题面、官方题解、学员代码、判题结论与最多若干条未通过用例（含隐藏用例，只在服务端使用）
    private AiAppealReviewDTO buildReviewRequest(TbUserSubmit submit) {
        TbQuestion question = questionMapper.selectById(submit.getQuestionId());
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        TbQuestionEditorial editorial = questionEditorialMapper.selectOne(new LambdaQueryWrapper<TbQuestionEditorial>()
                .eq(TbQuestionEditorial::getQuestionId, question.getQuestionId()));
        JudgeStatusEnum verdict = JudgeStatusEnum.getByCode(submit.getJudgeStatus());

        AiAppealReviewDTO dto = new AiAppealReviewDTO();
        dto.setQuestionTitle(question.getTitle());
        dto.setQuestionContent(question.getContent());
        dto.setDefaultCode(question.getDefaultCode());
        dto.setEditorial(editorial == null ? null : editorial.getContent());
        dto.setUserCode(submit.getUserCode());
        dto.setVerdict(verdict == null ? "未通过" : verdict.getDesc());
        dto.setPassCount(submit.getPassCount());
        dto.setTotalCount(submit.getTotalCount());
        dto.setExeMessage(submit.getExeMessage());
        dto.setFailedCases(failedCases(submit, questionCaseService.listAll(question.getQuestionId())));
        return dto;
    }

    // 未通过用例：有逐用例结果时按记录取前几条，早期提交只有首个未通过用例；用例已被修改删除的跳过
    private List<AiAppealCaseDTO> failedCases(TbUserSubmit submit, List<TbQuestionCase> cases) {
        Map<Long, TbQuestionCase> caseById = cases.stream().collect(Collectors.toMap(TbQuestionCase::getCaseId, Function.identity()));
        List<FriendCaseResultVO> results = AppealConverter.parseCaseResults(submit.getCaseOutputs());
        List<AiAppealCaseDTO> failed = new ArrayList<>();
        if (results.isEmpty() && submit.getFailCaseId() != null) {
            FriendCaseResultVO first = new FriendCaseResultVO();
            first.setCaseId(submit.getFailCaseId());
            first.setPass(false);
            first.setOutput(submit.getFailOutput());
            results = List.of(first);
        }
        for (int i = 0; i < results.size() && failed.size() < FAILED_CASE_LIMIT; i++) {
            FriendCaseResultVO result = results.get(i);
            TbQuestionCase questionCase = caseById.get(result.getCaseId());
            if (!Boolean.FALSE.equals(result.getPass()) || questionCase == null) {
                continue;
            }
            AiAppealCaseDTO item = new AiAppealCaseDTO();
            item.setIndex(cases.indexOf(questionCase) + 1);
            item.setSample(QuestionCaseTypeEnum.SAMPLE.getCode().equals(questionCase.getIsSample()));
            item.setInput(questionCase.getJudgeInput());
            item.setExpectedOutput(questionCase.getJudgeOutput());
            item.setActualOutput(result.getOutput());
            failed.add(item);
        }
        return failed;
    }

    // 今日剩余次数
    private AppealQuotaVO quota(Long userId) {
        AppealQuotaVO vo = new AppealQuotaVO();
        vo.setReviewLimit(reviewDailyLimit);
        vo.setReviewRemaining(dailyQuotaManager.getRemaining(FriendCacheConstants.APPEAL_REVIEW_QUOTA_KEY, reviewDailyLimit, userId));
        vo.setAppealLimit(appealDailyLimit);
        vo.setAppealRemaining(dailyQuotaManager.getRemaining(FriendCacheConstants.APPEAL_QUOTA_KEY, appealDailyLimit, userId));
        return vo;
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
