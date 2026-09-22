package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.ExamQuestionMapper;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.QuestionCaseService;
import cn.nuonuoya.friend.service.UserSubmitService;
import cn.nuonuoya.friend.vo.QuestionRunResultVO;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

// 用户代码提交业务实现类（集成 RabbitMQ 异步判题）
@Slf4j
@Service
public class UserSubmitServiceImpl implements UserSubmitService {

    // 运行示例用例限流窗口（秒）
    private static final long RUN_LIMIT_SECONDS = 2L;

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
        TbUserSubmit submit = new TbUserSubmit();
        submit.setUserId(userId);
        submit.setQuestionId(submitDTO.getQuestionId());
        submit.setExamId(submitDTO.getExamId());
        // 目前只支持 Java，不采信客户端传入的语言类型
        submit.setProgramType(ProgramTypeEnum.JAVA.getCode());
        submit.setUserCode(submitDTO.getUserCode());
        submit.setPass(SubmitPassEnum.JUDGING.getCode());
        submit.setScore(0);
        submit.setPassCount(0);
        submit.setTotalCount(caseList.size());
        submit.setExeMessage("");
        submit.setCreateBy(userId);
        submit.setCreateTime(LocalDateTime.now());

        // 持久化落库至 tb_user_submit 表并提交事务，生成 submitId
        transactionTemplate.executeWithoutResult(status -> userSubmitMapper.insert(submit));

        // 组装跨服务判题请求 DTO
        JudgeRequestDTO requestDTO = buildJudgeRequest(question, userId, submit.getUserCode(), caseList);
        requestDTO.setSubmitId(submit.getSubmitId());
        requestDTO.setProgramType(submit.getProgramType());

        // 异步发送至 RabbitMQ 交换机与队列
        try {
            rabbitTemplate.convertAndSend(
                    JudgeMqConstants.JUDGE_EXCHANGE,
                    JudgeMqConstants.JUDGE_ROUTING_KEY,
                    requestDTO
            );
            log.info("成功投递判题消息到 RabbitMQ: submitId = {}, questionId = {}",
                    submit.getSubmitId(), question.getQuestionId());
        } catch (Exception e) {
            log.error("投递 RabbitMQ 判题消息失败, submitId = {}, error: {}", submit.getSubmitId(), e.getMessage(), e);
            submit.setPass(SubmitPassEnum.NOT_PASS.getCode());
            submit.setJudgeStatus(JudgeStatusEnum.SE.getCode());
            submit.setExeMessage("系统异常：判题任务队列投递失败");
            transactionTemplate.executeWithoutResult(status -> userSubmitMapper.updateById(submit));
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

        JudgeRequestDTO requestDTO = buildJudgeRequest(question, userId, runDTO.getUserCode(), sampleList);
        requestDTO.setProgramType(ProgramTypeEnum.JAVA.getCode());
        JudgeResultVO judgeResult = judgeClient.run(requestDTO);

        QuestionRunResultVO vo = new QuestionRunResultVO();
        vo.setStatus(judgeResult.getStatus());
        vo.setPassCount(judgeResult.getPassCount());
        vo.setTotalCount(judgeResult.getTotalCount());
        vo.setTimeCost(judgeResult.getTimeCost());
        vo.setExeMessage(judgeResult.getExeMessage());
        vo.setCaseResults(QuestionCaseConverter.toCaseResultVOList(sampleList, judgeResult.getCaseResults()));
        return vo;
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
        return TableDataResult.success(UserSubmitConverter.toHistoryVOList(submitList), total);
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

    // 组装判题请求公共部分
    private JudgeRequestDTO buildJudgeRequest(TbQuestion question, Long userId, String userCode, List<TbQuestionCase> caseList) {
        JudgeRequestDTO requestDTO = new JudgeRequestDTO();
        requestDTO.setUserId(userId);
        requestDTO.setQuestionId(question.getQuestionId());
        requestDTO.setUserCode(userCode);
        requestDTO.setMainFunc(question.getMainFunc());
        // 时空限制为空时由判题服务使用默认值
        requestDTO.setTimeLimit(question.getTimeLimit());
        requestDTO.setSpaceLimit(question.getSpaceLimit());
        requestDTO.setDifficulty(question.getDifficulty());
        requestDTO.setCases(QuestionCaseConverter.toJudgeCaseList(caseList));
        return requestDTO;
    }

    // 将提交记录转换为结果视图（含首个未通过用例回显）
    private UserSubmitResultVO toResultVO(TbUserSubmit submit) {
        UserSubmitResultVO vo = new UserSubmitResultVO();
        vo.setSubmitId(submit.getSubmitId());
        vo.setQuestionId(submit.getQuestionId());
        vo.setExamId(submit.getExamId());
        vo.setProgramType(submit.getProgramType());
        vo.setPass(submit.getPass());
        vo.setExeMessage(submit.getExeMessage());
        vo.setScore(submit.getScore());
        vo.setStatus(submit.getJudgeStatus());
        vo.setPassCount(submit.getPassCount());
        vo.setTotalCount(submit.getTotalCount());
        vo.setTimeCost(submit.getTimeCost());
        vo.setCreateTime(submit.getCreateTime());
        vo.setCaseStates(submit.getCaseStates());
        if (submit.getFailCaseId() != null) {
            TbQuestionCase failCase = questionCaseService.getById(submit.getFailCaseId());
            vo.setFailCase(QuestionCaseConverter.toCaseResultVO(failCase, submit.getFailOutput(), false));
        }
        return vo;
    }
}
