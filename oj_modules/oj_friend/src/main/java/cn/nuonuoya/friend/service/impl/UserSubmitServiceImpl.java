package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.friend.vo.FriendRejudgeExamVO;
import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.api.friend.vo.FriendRejudgeResultVO;
import cn.nuonuoya.api.judge.constants.JudgeMqConstants;
import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.api.judge.enums.ProgramTypeEnum;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.friend.constants.FriendCacheConstants;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.client.JudgeClient;
import cn.nuonuoya.friend.converter.QuestionCaseConverter;
import cn.nuonuoya.friend.converter.UserSubmitConverter;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbExamQuestion;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.QuestionRunDTO;
import cn.nuonuoya.friend.dto.SubmitHistoryQueryDTO;
import cn.nuonuoya.friend.dto.UserSubmitDTO;
import cn.nuonuoya.friend.enums.ExamPublishStatusEnum;
import cn.nuonuoya.friend.enums.ExamRankSettledEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.ExamQuestionMapper;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.AppealService;
import cn.nuonuoya.friend.service.QuestionCaseService;
import cn.nuonuoya.friend.service.UserSubmitService;
import cn.nuonuoya.friend.vo.QuestionRunResultVO;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import cn.nuonuoya.friend.vo.UserSubmitResultVO;
import cn.nuonuoya.security.utils.SecurityUtils;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// 用户代码提交业务实现类（集成 RabbitMQ 异步判题）
@Slf4j
@Service
public class UserSubmitServiceImpl implements UserSubmitService {

    // 运行示例用例限流窗口（秒）
    private static final long RUN_LIMIT_SECONDS = 2L;

    // 重判时每批读取的提交数
    private static final int REJUDGE_BATCH_SIZE = 200;

    // 判题消息投递失败时写入的回显
    private static final String DELIVER_FAILED_MESSAGE = "系统异常：判题任务队列投递失败";

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Autowired
    private QuestionCaseService questionCaseService;

    @Autowired
    private JudgeClient judgeClient;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AppealService appealService;

    // 提交代码、落库初始化记录并向 RabbitMQ 投递异步判题任务（全部用例）
    // 记录先独立提交再投递消息，避免判题结果先于记录提交回写而丢失
    @Override
    public UserSubmitResultVO submit(UserSubmitDTO submitDTO) {
        if (submitDTO == null || submitDTO.getQuestionId() == null || StrUtil.isBlank(submitDTO.getUserCode())) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 从认证上下文提取用户身份，严格保障安全性
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        // 查询题目元数据与全部用例
        TbQuestion question = questionMapper.selectById(submitDTO.getQuestionId());
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        List<TbQuestionCase> caseList = questionCaseService.listAll(question.getQuestionId());
        if (CollUtil.isEmpty(caseList)) {
            throw new ServiceException(ResultCode.FAILED_QUESTION_NO_CASE);
        }

        // 竞赛提交计入排名，必须已报名、处于比赛时间内且题目属于该竞赛
        if (submitDTO.getExamId() != null) {
            validateExamSubmit(userId, submitDTO.getExamId(), question.getQuestionId());
        }

        // 初始化提交记录（状态为评测中）
        TbUserSubmit submit = BeanUtil.copyProperties(submitDTO, TbUserSubmit.class);
        submit.setUserId(userId);
        // 目前只支持 Java，不采信客户端传入的语言类型
        submit.setProgramType(ProgramTypeEnum.JAVA.getCode());
        submit.setPass(SubmitPassEnum.JUDGING.getCode());
        submit.setScore(0);
        submit.setPassCount(0);
        submit.setTotalCount(caseList.size());
        submit.setExeMessage("");
        submit.setCreateBy(userId);
        submit.setCreateTime(LocalDateTime.now());

        // 持久化落库至 tb_user_submit 表并提交事务，生成 submitId
        transactionTemplate.executeWithoutResult(status -> userSubmitMapper.insert(submit));

        // 异步发送至 RabbitMQ 交换机与队列，失败时记录已标记为系统错误
        if (!deliverJudgeRequest(buildJudgeRequest(question, submit, caseList))) {
            submit.setPass(SubmitPassEnum.NOT_PASS.getCode());
            submit.setJudgeStatus(JudgeStatusEnum.SE.getCode());
            submit.setExeMessage(DELIVER_FAILED_MESSAGE);
        }

        return toResultVO(submit);
    }

    // 同步运行公开示例用例（不落库、不计分）
    @Override
    public QuestionRunResultVO run(QuestionRunDTO runDTO) {
        if (runDTO == null || runDTO.getQuestionId() == null || StrUtil.isBlank(runDTO.getUserCode())) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        // 按用户限流（原子写入带过期时间的标记），避免高频占用沙箱容器
        String limitKey = FriendCacheConstants.QUESTION_RUN_LIMIT_KEY + userId;
        if (!redisService.setIfAbsent(limitKey, 1, RUN_LIMIT_SECONDS, TimeUnit.SECONDS)) {
            throw new ServiceException(ResultCode.FAILED_FREQUENT);
        }

        TbQuestion question = questionMapper.selectById(runDTO.getQuestionId());
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        List<TbQuestionCase> sampleList = questionCaseService.listSamples(question.getQuestionId());
        if (CollUtil.isEmpty(sampleList)) {
            throw new ServiceException(ResultCode.FAILED_QUESTION_NO_CASE);
        }

        JudgeResultVO judgeResult = judgeClient.run(buildJudgeRequest(question, userId, runDTO.getUserCode(), sampleList));
        return UserSubmitConverter.toRunResultVO(judgeResult, sampleList);
    }

    // 查询提交记录评测结果详情（仅本人可查）
    @Override
    public UserSubmitResultVO getSubmitResult(Long submitId) {
        if (submitId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        TbUserSubmit submit = userSubmitMapper.selectById(submitId);
        if (submit == null || !userId.equals(submit.getUserId())) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        return toResultVO(submit);
    }

    // 回写异步判题结果（重复投递时覆盖写入，结果一致）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveJudgeResult(JudgeResultVO resultVO) {
        int rows = userSubmitMapper.updateById(UserSubmitConverter.toJudgedEntity(resultVO));
        if (rows == 0) {
            log.warn("判题结果对应的提交记录不存在: submitId = {}", resultVO.getSubmitId());
        }
    }

    // 分页查询当前用户本题的提交记录（按提交时间倒序）
    @Override
    public TableDataResult<SubmitHistoryVO> listHistory(SubmitHistoryQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getQuestionId() == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<TbUserSubmit> submitList = userSubmitMapper.selectList(new LambdaQueryWrapper<TbUserSubmit>()
                .eq(TbUserSubmit::getUserId, userId)
                .eq(TbUserSubmit::getQuestionId, queryDTO.getQuestionId())
                .orderByDesc(TbUserSubmit::getCreateTime)
                .orderByDesc(TbUserSubmit::getSubmitId));
        if (CollUtil.isEmpty(submitList)) {
            return TableDataResult.empty();
        }
        long total = new PageInfo<>(submitList).getTotal();
        List<SubmitHistoryVO> voList = UserSubmitConverter.toHistoryVOList(submitList);
        appealService.fillHistoryAppeal(voList, submitList);
        return TableDataResult.success(voList, total);
    }

    // 预览按题重判的影响范围（与重判使用同一套范围规则）
    @Override
    public FriendRejudgePreviewVO previewRejudge(Long questionId) {
        RejudgeScope scope = resolveRejudgeScope(questionId);
        FriendRejudgePreviewVO vo = new FriendRejudgePreviewVO();
        vo.setRejudgeCount(scope.targetIds().size());
        vo.setPracticeCount(scope.practiceCount());
        vo.setSettledCount(scope.settledCount());
        vo.setJudgingCount(scope.judgingCount());
        LocalDateTime now = LocalDateTime.now();
        List<FriendRejudgeExamVO> exams = new ArrayList<>(scope.examCounts().size());
        scope.examCounts().forEach((examId, count) -> {
            TbExam exam = scope.unsettledExams().get(examId);
            FriendRejudgeExamVO examVO = new FriendRejudgeExamVO();
            examVO.setExamId(examId);
            examVO.setTitle(exam.getTitle());
            examVO.setFinished(exam.getEndTime() != null && !now.isBefore(exam.getEndTime()));
            examVO.setCount(count);
            exams.add(examVO);
        });
        vo.setExams(exams);
        return vo;
    }

    // 按题重判：练习提交与未结算竞赛的提交逐条抢占为评测中再投递，已在评测中的跳过，保证重复点击不重复投递
    @Override
    public FriendRejudgeResultVO rejudge(Long questionId) {
        FriendRejudgeResultVO result = new FriendRejudgeResultVO();
        result.setQueuedCount(0);
        result.setDeliverFailed(false);
        TbQuestion question = questionId == null ? null : questionMapper.selectById(questionId);
        if (question == null) {
            return result;
        }
        List<TbQuestionCase> caseList = questionCaseService.listAll(questionId);
        if (CollUtil.isEmpty(caseList)) {
            return result;
        }
        RejudgeScope scope = resolveRejudgeScope(questionId);
        int queued = 0;
        outer:
        for (List<Long> batchIds : ListUtil.partition(scope.targetIds(), REJUDGE_BATCH_SIZE)) {
            List<TbUserSubmit> batch = userSubmitMapper.selectList(new LambdaQueryWrapper<TbUserSubmit>()
                    .select(TbUserSubmit::getSubmitId, TbUserSubmit::getUserId, TbUserSubmit::getUserCode,
                            TbUserSubmit::getProgramType)
                    .in(TbUserSubmit::getSubmitId, batchIds));
            for (TbUserSubmit submit : batch) {
                if (!claimForRejudge(submit.getSubmitId(), caseList.size())) {
                    continue;
                }
                if (!deliverJudgeRequest(buildJudgeRequest(question, submit, caseList))) {
                    // 队列不可用时停止，避免把剩余记录都改成系统错误；剩余记录保持原结论，恢复后可再次重判
                    result.setDeliverFailed(true);
                    break outer;
                }
                queued++;
            }
        }
        result.setQueuedCount(queued);
        // 排名缓存按新结论重新计算
        scope.examCounts().keySet().forEach(examId -> redisService.deleteObject(FriendCacheConstants.EXAM_RANK_LIST_KEY + examId));
        log.info("按题重判完成, questionId = {}, 投递 = {}, 投递失败中止 = {}", questionId, queued, result.getDeliverFailed());
        return result;
    }

    // 读取该题全部提交的竞赛归属与状态，按重判范围归类（练习与未结算竞赛重判，已结算或已删除的竞赛与评测中的跳过）
    private RejudgeScope resolveRejudgeScope(Long questionId) {
        if (questionId == null) {
            return new RejudgeScope(Collections.emptyList(), 0, 0, 0, Collections.emptyMap(), Collections.emptyMap());
        }
        List<TbUserSubmit> submits = userSubmitMapper.selectList(new LambdaQueryWrapper<TbUserSubmit>()
                .select(TbUserSubmit::getSubmitId, TbUserSubmit::getExamId, TbUserSubmit::getPass)
                .eq(TbUserSubmit::getQuestionId, questionId)
                .orderByAsc(TbUserSubmit::getSubmitId));
        Set<Long> examIds = submits.stream()
                .map(TbUserSubmit::getExamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, TbExam> unsettledExams = examIds.isEmpty()
                ? Collections.emptyMap()
                : examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                        .in(TbExam::getExamId, examIds)
                        .eq(TbExam::getRankSettled, ExamRankSettledEnum.UNSETTLED.getCode()))
                .stream()
                .collect(Collectors.toMap(TbExam::getExamId, exam -> exam));

        List<Long> targetIds = new ArrayList<>();
        Map<Long, Integer> examCounts = new LinkedHashMap<>();
        int practiceCount = 0;
        int settledCount = 0;
        int judgingCount = 0;
        for (TbUserSubmit submit : submits) {
            if (SubmitPassEnum.JUDGING.getCode().equals(submit.getPass())) {
                judgingCount++;
            } else if (submit.getExamId() == null) {
                practiceCount++;
                targetIds.add(submit.getSubmitId());
            } else if (unsettledExams.containsKey(submit.getExamId())) {
                examCounts.merge(submit.getExamId(), 1, Integer::sum);
                targetIds.add(submit.getSubmitId());
            } else {
                settledCount++;
            }
        }
        return new RejudgeScope(targetIds, practiceCount, settledCount, judgingCount, examCounts, unsettledExams);
    }

    // 抢占式把提交改回评测中并清空上次结论（条件更新，已在评测中的返回 false）
    private boolean claimForRejudge(Long submitId, int totalCount) {
        Integer rows = transactionTemplate.execute(status -> userSubmitMapper.update(null, new LambdaUpdateWrapper<TbUserSubmit>()
                .set(TbUserSubmit::getPass, SubmitPassEnum.JUDGING.getCode())
                .set(TbUserSubmit::getJudgeStatus, null)
                .set(TbUserSubmit::getScore, 0)
                .set(TbUserSubmit::getPassCount, 0)
                .set(TbUserSubmit::getTotalCount, totalCount)
                .set(TbUserSubmit::getTimeCost, null)
                .set(TbUserSubmit::getExeMessage, "")
                .set(TbUserSubmit::getFailCaseId, null)
                .set(TbUserSubmit::getFailOutput, null)
                .set(TbUserSubmit::getCaseStates, null)
                .set(TbUserSubmit::getCaseOutputs, null)
                .set(TbUserSubmit::getUpdateTime, LocalDateTime.now())
                .eq(TbUserSubmit::getSubmitId, submitId)
                .ne(TbUserSubmit::getPass, SubmitPassEnum.JUDGING.getCode())));
        return rows != null && rows > 0;
    }

    // 投递判题消息，失败时把该提交标记为系统错误；投递成功返回 true
    private boolean deliverJudgeRequest(JudgeRequestDTO requestDTO) {
        try {
            rabbitTemplate.convertAndSend(
                    JudgeMqConstants.JUDGE_EXCHANGE,
                    JudgeMqConstants.JUDGE_ROUTING_KEY,
                    requestDTO
            );
            log.info("成功投递判题消息到 RabbitMQ: submitId = {}, questionId = {}",
                    requestDTO.getSubmitId(), requestDTO.getQuestionId());
            return true;
        } catch (Exception e) {
            log.error("投递 RabbitMQ 判题消息失败, submitId = {}, error: {}", requestDTO.getSubmitId(), e.getMessage(), e);
            transactionTemplate.executeWithoutResult(status -> userSubmitMapper.update(null, new LambdaUpdateWrapper<TbUserSubmit>()
                    .set(TbUserSubmit::getPass, SubmitPassEnum.NOT_PASS.getCode())
                    .set(TbUserSubmit::getJudgeStatus, JudgeStatusEnum.SE.getCode())
                    .set(TbUserSubmit::getExeMessage, DELIVER_FAILED_MESSAGE)
                    .set(TbUserSubmit::getUpdateTime, LocalDateTime.now())
                    .eq(TbUserSubmit::getSubmitId, requestDTO.getSubmitId())));
            return false;
        }
    }

    // 按题重判的范围：待重判的提交、各类计数与涉及的未结算竞赛
    private record RejudgeScope(List<Long> targetIds, int practiceCount, int settledCount, int judgingCount,
                                Map<Long, Integer> examCounts, Map<Long, TbExam> unsettledExams) {
    }

    // 校验竞赛提交资格（赛后练习不带竞赛ID提交，不影响排名）
    private void validateExamSubmit(Long userId, Long examId, Long questionId) {
        TbExam exam = examMapper.selectById(examId);
        if (exam == null || !ExamPublishStatusEnum.PUBLISHED.getCode().equals(exam.getStatus())) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_NOT_STARTED);
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_IS_FINISHED);
        }
        Long enrolled = userExamMapper.selectCount(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getExamId, examId)
                .eq(TbUserExam::getUserId, userId));
        if (enrolled == null || enrolled == 0) {
            throw new ServiceException(ResultCode.FAILED_EXAM_NOT_ENROLLED);
        }
        Long inExam = examQuestionMapper.selectCount(new LambdaQueryWrapper<TbExamQuestion>()
                .eq(TbExamQuestion::getExamId, examId)
                .eq(TbExamQuestion::getQuestionId, questionId));
        if (inExam == null || inExam == 0) {
            throw new ServiceException(ResultCode.FAILED_EXAM_QUESTION_NOT_IN);
        }
    }

    // 组装运行示例用例的判题请求（题目主键、main 函数、时空限制与难度同名拷贝；时空限制为空时由判题服务使用默认值）
    private JudgeRequestDTO buildJudgeRequest(TbQuestion question, Long userId, String userCode, List<TbQuestionCase> caseList) {
        JudgeRequestDTO requestDTO = BeanUtil.copyProperties(question, JudgeRequestDTO.class);
        requestDTO.setUserId(userId);
        requestDTO.setUserCode(userCode);
        requestDTO.setProgramType(ProgramTypeEnum.JAVA.getCode());
        requestDTO.setCases(QuestionCaseConverter.toJudgeCaseList(caseList));
        return requestDTO;
    }

    // 组装一条提交记录的判题请求（提交与重判共用）
    private JudgeRequestDTO buildJudgeRequest(TbQuestion question, TbUserSubmit submit, List<TbQuestionCase> caseList) {
        JudgeRequestDTO requestDTO = buildJudgeRequest(question, submit.getUserId(), submit.getUserCode(), caseList);
        requestDTO.setSubmitId(submit.getSubmitId());
        requestDTO.setProgramType(submit.getProgramType());
        return requestDTO;
    }

    // 将提交记录转换为结果视图（含首个未通过用例回显）
    private UserSubmitResultVO toResultVO(TbUserSubmit submit) {
        UserSubmitResultVO vo = UserSubmitConverter.toResultVO(submit);
        if (submit.getFailCaseId() != null) {
            TbQuestionCase failCase = questionCaseService.getById(submit.getFailCaseId());
            vo.setFailCase(QuestionCaseConverter.toCaseResultVO(failCase, submit.getFailOutput(), false));
        }
        return vo;
    }
}
