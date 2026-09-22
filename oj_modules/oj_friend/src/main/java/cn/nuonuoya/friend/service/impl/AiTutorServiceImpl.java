package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.ai.constants.AiInternalPaths;
import cn.nuonuoya.api.ai.dto.AiTutorChatDTO;
import cn.nuonuoya.api.ai.dto.AiTutorHistoryDTO;
import cn.nuonuoya.api.ai.dto.AiTutorSampleDTO;
import cn.nuonuoya.api.ai.dto.AiTutorSubmissionDTO;
import cn.nuonuoya.api.ai.enums.AiTutorActionEnum;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.cache.AiTutorQuotaManager;
import cn.nuonuoya.friend.client.AiTutorClient;
import cn.nuonuoya.friend.converter.AiTutorConverter;
import cn.nuonuoya.friend.domain.TbAiChatMessage;
import cn.nuonuoya.friend.domain.TbAiChatSession;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbExamQuestion;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.AiTutorAskDTO;
import cn.nuonuoya.friend.enums.AiChatRoleEnum;
import cn.nuonuoya.friend.enums.ExamPublishStatusEnum;
import cn.nuonuoya.friend.enums.QuestionCaseTypeEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.AiChatMessageMapper;
import cn.nuonuoya.friend.mapper.AiChatSessionMapper;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.ExamQuestionMapper;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.AiTutorService;
import cn.nuonuoya.friend.service.QuestionCaseService;
import cn.nuonuoya.friend.vo.AiTutorSessionVO;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.utils.SecurityUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// AI 做题辅导实现：资格、次数、上下文与落库都在这里完成，AI 服务只负责生成回复
@Slf4j
@Service
public class AiTutorServiceImpl implements AiTutorService {

    // 会话页展示的最近消息条数
    private static final int SESSION_MESSAGE_LIMIT = 50;

    // 发送给模型的最近历史条数（10 轮）
    private static final int HISTORY_MESSAGE_LIMIT = 20;

    // 发送给模型的公开示例上限
    private static final int SAMPLE_LIMIT = 5;

    // 发送给模型的编译或运行信息最大长度
    private static final int EXE_MESSAGE_LIMIT = 2000;

    // SSE 连接最长保持时间（毫秒）
    private static final long EMITTER_TIMEOUT_MS = 150_000L;

    // 发给浏览器的事件名：增量文本、结束、失败
    private static final String EVENT_DELTA = "delta";
    private static final String EVENT_DONE = "done";
    private static final String EVENT_ERROR = "error";

    @Autowired
    private AiTutorClient aiTutorClient;

    @Autowired
    private AiTutorQuotaManager aiTutorQuotaManager;

    @Autowired
    private AiChatSessionMapper aiChatSessionMapper;

    @Autowired
    private AiChatMessageMapper aiChatMessageMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionCaseService questionCaseService;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // 查询会话：历史消息、剩余次数与快捷操作所需的提交状态
    @Override
    public AiTutorSessionVO getSession(Long questionId) {
        Long userId = requireUserId();
        requireQuestion(questionId);

        AiTutorSessionVO vo = new AiTutorSessionVO();
        TbAiChatSession session = findSession(userId, questionId);
        if (session == null) {
            vo.setMessages(Collections.emptyList());
        } else {
            List<TbAiChatMessage> recent = aiChatMessageMapper.selectList(new LambdaQueryWrapper<TbAiChatMessage>()
                    .eq(TbAiChatMessage::getSessionId, session.getSessionId())
                    .orderByDesc(TbAiChatMessage::getCreateTime)
                    .orderByDesc(TbAiChatMessage::getMessageId)
                    .last("LIMIT " + SESSION_MESSAGE_LIMIT));
            Collections.reverse(recent);
            vo.setMessages(AiTutorConverter.toMessageVOList(recent));
        }
        vo.setDailyLimit(aiTutorQuotaManager.getDailyLimit());
        vo.setRemaining(aiTutorQuotaManager.getRemaining(userId));
        TbUserSubmit latest = latestFinishedSubmit(userId, questionId);
        vo.setLatestJudgeStatus(latest == null ? null : latest.getJudgeStatus());
        vo.setAccepted(latestAcceptedSubmit(userId, questionId) != null);
        vo.setAvailable(!inOngoingExam(userId, questionId));
        return vo;
    }

    // 提问：同步完成全部校验与上下文组装，再异步转发模型的流式回复
    @Override
    public SseEmitter ask(Long questionId, AiTutorAskDTO askDTO) {
        Long userId = requireUserId();
        TbQuestion question = requireQuestion(questionId);
        AiTutorActionEnum action = AiTutorActionEnum.getByCode(askDTO.getAction());
        if (action == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        String content = StrUtil.trimToEmpty(askDTO.getContent());
        if (action == AiTutorActionEnum.CHAT && content.isEmpty()) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE, "请输入问题");
        }
        if (inOngoingExam(userId, questionId)) {
            throw new ServiceException(ResultCode.FAILED_AI_IN_EXAM);
        }

        AiTutorChatDTO chatDTO = buildChatDTO(userId, question, action, content, askDTO.getUserCode());
        Long sessionId = getOrCreateSession(userId, questionId);
        if (!aiTutorQuotaManager.tryAcquire(userId)) {
            throw new ServiceException(ResultCode.FAILED_AI_QUOTA_EXCEEDED);
        }
        chatDTO.setHistory(loadHistory(sessionId));

        String userMessage = content.isEmpty() ? action.getLabel() : content;
        return relay(userId, sessionId, action, userMessage, chatDTO);
    }

    // 把 AI 服务的流式事件转发给浏览器；正常结束时落库并返回剩余次数，失败时归还次数
    private SseEmitter relay(Long userId, Long sessionId, AiTutorActionEnum action, String userMessage, AiTutorChatDTO chatDTO) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        StringBuilder reply = new StringBuilder();
        JSONObject[] doneData = new JSONObject[1];
        boolean[] failed = new boolean[1];

        Disposable subscription = aiTutorClient.streamChat(chatDTO).subscribe(
                event -> {
                    if (failed[0]) {
                        return;
                    }
                    if (AiInternalPaths.EVENT_DELTA.equals(event.event())) {
                        String text = JSON.parseObject(event.data()).getString("text");
                        reply.append(text);
                        send(emitter, EVENT_DELTA, new JSONObject().fluentPut("text", text));
                    } else if (AiInternalPaths.EVENT_DONE.equals(event.event())) {
                        doneData[0] = JSON.parseObject(event.data());
                    } else if (AiInternalPaths.EVENT_ERROR.equals(event.event())) {
                        failed[0] = true;
                    }
                },
                error -> finishWithError(emitter, userId),
                () -> {
                    if (failed[0] || doneData[0] == null || reply.isEmpty()) {
                        finishWithError(emitter, userId);
                        return;
                    }
                    try {
                        Long messageId = saveRound(userId, sessionId, action, userMessage, reply.toString(), doneData[0]);
                        send(emitter, EVENT_DONE, new JSONObject()
                                .fluentPut("messageId", String.valueOf(messageId))
                                .fluentPut("remaining", aiTutorQuotaManager.getRemaining(userId)));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("保存 AI 辅导记录失败, userId = {}, sessionId = {}", userId, sessionId, e);
                        send(emitter, EVENT_ERROR, new JSONObject().fluentPut("msg", "回复保存失败，请稍后重试"));
                        emitter.complete();
                    }
                });

        // 浏览器断开或超时时停止拉取模型回复（已产生的调用照常计入次数）
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());
        emitter.onCompletion(subscription::dispose);
        return emitter;
    }

    // 以失败结束：归还次数并通知浏览器
    private void finishWithError(SseEmitter emitter, Long userId) {
        aiTutorQuotaManager.release(userId);
        send(emitter, EVENT_ERROR, new JSONObject().fluentPut("msg", ResultCode.FAILED_AI_BUSY.getMsg()));
        emitter.complete();
    }

    // 发送一个 SSE 事件，浏览器已断开时忽略
    private void send(SseEmitter emitter, String name, JSONObject data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data.toJSONString()));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 发送失败（浏览器可能已断开）: {}", e.getMessage());
        }
    }

    // 保存一轮问答（运行在流式回调线程，审计字段手动填充），返回 AI 消息 id
    private Long saveRound(Long userId, Long sessionId, AiTutorActionEnum action, String question, String answer, JSONObject done) {
        return transactionTemplate.execute(status -> {
            LocalDateTime now = LocalDateTime.now();
            TbAiChatMessage userMsg = newMessage(userId, sessionId, action, AiChatRoleEnum.USER, question, now);
            aiChatMessageMapper.insert(userMsg);
            TbAiChatMessage aiMsg = newMessage(userId, sessionId, action, AiChatRoleEnum.ASSISTANT, answer, now.plusNanos(1_000_000));
            aiMsg.setModel(done.getString("model"));
            aiMsg.setPromptTokens(done.getInteger("promptTokens"));
            aiMsg.setCompletionTokens(done.getInteger("completionTokens"));
            aiChatMessageMapper.insert(aiMsg);
            return aiMsg.getMessageId();
        });
    }

    // 构造一条消息实体
    private TbAiChatMessage newMessage(Long userId, Long sessionId, AiTutorActionEnum action, AiChatRoleEnum role,
                                       String content, LocalDateTime createTime) {
        TbAiChatMessage message = new TbAiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role.getCode());
        message.setAction(action.getCode());
        message.setContent(content);
        message.setCreateBy(userId);
        message.setCreateTime(createTime);
        return message;
    }

    // 组装发给 AI 服务的上下文（题面、示例、当前代码、被分析的提交）
    private AiTutorChatDTO buildChatDTO(Long userId, TbQuestion question, AiTutorActionEnum action, String content, String userCode) {
        AiTutorChatDTO dto = new AiTutorChatDTO();
        dto.setAction(action.getCode());
        dto.setQuestionTitle(question.getTitle());
        dto.setQuestionContent(question.getContent());
        dto.setDefaultCode(question.getDefaultCode());
        dto.setUserCode(StrUtil.isBlank(userCode) ? null : userCode);
        dto.setMessage(content.isEmpty() ? null : content);
        List<TbQuestionCase> allCases = questionCaseService.listAll(question.getQuestionId());
        dto.setSamples(allCases.stream()
                .filter(c -> QuestionCaseTypeEnum.SAMPLE.getCode().equals(c.getIsSample()))
                .limit(SAMPLE_LIMIT)
                .map(c -> {
                    AiTutorSampleDTO sample = new AiTutorSampleDTO();
                    sample.setInput(c.getDisplayInput());
                    sample.setOutput(c.getDisplayOutput());
                    return sample;
                })
                .toList());

        TbUserSubmit submit = switch (action) {
            case ANALYZE_SUBMIT -> requireSubmit(latestFinishedSubmit(userId, question.getQuestionId()),
                    s -> !JudgeStatusEnum.AC.getCode().equals(s.getJudgeStatus()));
            case EXPLAIN_COMPILE -> requireSubmit(latestFinishedSubmit(userId, question.getQuestionId()),
                    s -> JudgeStatusEnum.CE.getCode().equals(s.getJudgeStatus()));
            case REVIEW_CODE -> requireSubmit(latestAcceptedSubmit(userId, question.getQuestionId()), s -> true);
            default -> null;
        };
        if (submit != null) {
            dto.setSubmission(toSubmissionDTO(submit, allCases));
        }
        return dto;
    }

    // 校验提交存在且符合快捷操作要求
    private TbUserSubmit requireSubmit(TbUserSubmit submit, Predicate<TbUserSubmit> condition) {
        if (submit == null || !condition.test(submit)) {
            throw new ServiceException(ResultCode.FAILED_AI_ACTION_UNAVAILABLE);
        }
        return submit;
    }

    // 提交记录转换为 AI 上下文：隐藏用例只给序号，不给输入与预期输出
    private AiTutorSubmissionDTO toSubmissionDTO(TbUserSubmit submit, List<TbQuestionCase> allCases) {
        AiTutorSubmissionDTO dto = new AiTutorSubmissionDTO();
        dto.setCode(submit.getUserCode());
        JudgeStatusEnum status = JudgeStatusEnum.getByCode(submit.getJudgeStatus());
        dto.setVerdict(status == null ? null : status.getDesc());
        dto.setPassCount(submit.getPassCount());
        dto.setTotalCount(submit.getTotalCount());
        dto.setExeMessage(StrUtil.maxLength(submit.getExeMessage(), EXE_MESSAGE_LIMIT));
        if (submit.getFailCaseId() != null) {
            for (int i = 0; i < allCases.size(); i++) {
                TbQuestionCase c = allCases.get(i);
                if (!Objects.equals(c.getCaseId(), submit.getFailCaseId())) {
                    continue;
                }
                dto.setFailCaseIndex(i + 1);
                boolean sample = QuestionCaseTypeEnum.SAMPLE.getCode().equals(c.getIsSample());
                dto.setFailCaseSample(sample);
                if (sample) {
                    dto.setFailInput(c.getDisplayInput());
                    dto.setFailExpected(c.getDisplayOutput());
                    dto.setFailActual(submit.getFailOutput());
                }
                break;
            }
        }
        return dto;
    }

    // 读取最近的历史对话（按时间升序，最多 10 轮）
    private List<AiTutorHistoryDTO> loadHistory(Long sessionId) {
        List<TbAiChatMessage> recent = aiChatMessageMapper.selectList(new LambdaQueryWrapper<TbAiChatMessage>()
                .eq(TbAiChatMessage::getSessionId, sessionId)
                .orderByDesc(TbAiChatMessage::getCreateTime)
                .orderByDesc(TbAiChatMessage::getMessageId)
                .last("LIMIT " + HISTORY_MESSAGE_LIMIT));
        Collections.reverse(recent);
        List<AiTutorHistoryDTO> history = new ArrayList<>(recent.size());
        for (TbAiChatMessage message : recent) {
            AiTutorHistoryDTO item = new AiTutorHistoryDTO();
            item.setFromUser(AiChatRoleEnum.USER.getCode().equals(message.getRole()));
            item.setContent(message.getContent());
            history.add(item);
        }
        return history;
    }

    // 获取或创建会话（并发创建时以唯一索引兜底）
    private Long getOrCreateSession(Long userId, Long questionId) {
        TbAiChatSession session = findSession(userId, questionId);
        if (session != null) {
            return session.getSessionId();
        }
        TbAiChatSession created = new TbAiChatSession();
        created.setUserId(userId);
        created.setQuestionId(questionId);
        try {
            aiChatSessionMapper.insert(created);
            return created.getSessionId();
        } catch (DuplicateKeyException e) {
            return findSession(userId, questionId).getSessionId();
        }
    }

    // 查询会话
    private TbAiChatSession findSession(Long userId, Long questionId) {
        return aiChatSessionMapper.selectOne(new LambdaQueryWrapper<TbAiChatSession>()
                .eq(TbAiChatSession::getUserId, userId)
                .eq(TbAiChatSession::getQuestionId, questionId));
    }

    // 最近一次已出结果的提交
    private TbUserSubmit latestFinishedSubmit(Long userId, Long questionId) {
        return userSubmitMapper.selectOne(new LambdaQueryWrapper<TbUserSubmit>()
                .eq(TbUserSubmit::getUserId, userId)
                .eq(TbUserSubmit::getQuestionId, questionId)
                .ne(TbUserSubmit::getPass, SubmitPassEnum.JUDGING.getCode())
                .isNotNull(TbUserSubmit::getJudgeStatus)
                .orderByDesc(TbUserSubmit::getCreateTime)
                .last("LIMIT 1"));
    }

    // 最近一次通过的提交
    private TbUserSubmit latestAcceptedSubmit(Long userId, Long questionId) {
        return userSubmitMapper.selectOne(new LambdaQueryWrapper<TbUserSubmit>()
                .eq(TbUserSubmit::getUserId, userId)
                .eq(TbUserSubmit::getQuestionId, questionId)
                .eq(TbUserSubmit::getJudgeStatus, JudgeStatusEnum.AC.getCode())
                .orderByDesc(TbUserSubmit::getCreateTime)
                .last("LIMIT 1"));
    }

    // 用户已报名且正在进行的竞赛中是否包含本题（不依赖前端是否携带竞赛ID）
    private boolean inOngoingExam(Long userId, Long questionId) {
        Set<Long> enrolledExamIds = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                        .select(TbUserExam::getExamId)
                        .eq(TbUserExam::getUserId, userId))
                .stream().map(TbUserExam::getExamId).collect(Collectors.toSet());
        if (enrolledExamIds.isEmpty()) {
            return false;
        }
        List<Long> examIds = examQuestionMapper.selectList(new LambdaQueryWrapper<TbExamQuestion>()
                        .select(TbExamQuestion::getExamId)
                        .eq(TbExamQuestion::getQuestionId, questionId)
                        .in(TbExamQuestion::getExamId, enrolledExamIds))
                .stream().map(TbExamQuestion::getExamId).toList();
        if (CollUtil.isEmpty(examIds)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return examMapper.selectCount(new LambdaQueryWrapper<TbExam>()
                .in(TbExam::getExamId, examIds)
                .eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode())
                .le(TbExam::getStartTime, now)
                .ge(TbExam::getEndTime, now)) > 0;
    }

    // 当前登录用户（接口不在网关白名单内，未登录请求到不了这里，此处为兜底）
    private Long requireUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        return userId;
    }

    // 查询题目，不存在时报错
    private TbQuestion requireQuestion(Long questionId) {
        TbQuestion question = questionId == null ? null : questionMapper.selectById(questionId);
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        return question;
    }
}
