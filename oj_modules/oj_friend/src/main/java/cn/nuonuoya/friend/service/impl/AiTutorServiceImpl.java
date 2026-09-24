package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.ai.constants.AiInternalPaths;
import cn.nuonuoya.api.ai.dto.AiTutorChatDTO;
import cn.nuonuoya.api.ai.enums.AiTutorActionEnum;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.cache.AiTutorQuotaManager;
import cn.nuonuoya.friend.client.AiTutorClient;
import cn.nuonuoya.friend.converter.AiTutorConverter;
import cn.nuonuoya.friend.domain.TbAiChatMessage;
import cn.nuonuoya.friend.domain.TbAiChatSession;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.AiTutorAskDTO;
import cn.nuonuoya.friend.enums.AiChatRoleEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.AiChatMessageMapper;
import cn.nuonuoya.friend.mapper.AiChatSessionMapper;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.AiTutorService;
import cn.nuonuoya.friend.service.CodeDraftService;
import cn.nuonuoya.friend.service.ExamService;
import cn.nuonuoya.friend.service.QuestionCaseService;
import cn.nuonuoya.friend.vo.AiTutorSessionVO;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.utils.SecurityUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
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
import java.util.function.Predicate;

// AI 做题辅导实现：资格、次数、落库与转发在这里完成，上下文组装在转换器，竞赛判断在竞赛服务（只在竞赛中答题时禁用，普通练习不受影响）
@Slf4j
@Service
public class AiTutorServiceImpl implements AiTutorService {

    // 会话页展示的最近消息条数
    private static final int SESSION_MESSAGE_LIMIT = 50;

    // 发送给模型的最近历史条数（10 轮）
    private static final int HISTORY_MESSAGE_LIMIT = 20;

    // SSE 连接最长保持时间（毫秒）
    private static final long EMITTER_TIMEOUT_MS = 150_000L;

    // 发给浏览器的结束事件字段：AI 消息 id
    private static final String FIELD_MESSAGE_ID = "messageId";

    // 发给浏览器的结束事件字段：今日剩余次数
    private static final String FIELD_REMAINING = "remaining";

    // AI 消息的创建时间相对用户消息的偏移（纳秒），保证同一轮内排序稳定
    private static final long REPLY_TIME_OFFSET_NANOS = 1_000_000L;

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
    private ExamService examService;

    @Autowired
    private CodeDraftService codeDraftService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // 查询会话：历史消息、剩余次数与快捷操作所需的提交状态
    @Override
    public AiTutorSessionVO getSession(Long questionId, Long examId) {
        Long userId = requireUserId();
        requireQuestion(questionId);

        AiTutorSessionVO vo = new AiTutorSessionVO();
        TbAiChatSession session = findSession(userId, questionId);
        vo.setMessages(session == null ? Collections.emptyList()
                : AiTutorConverter.toMessageVOList(recentMessages(session.getSessionId(), SESSION_MESSAGE_LIMIT)));
        vo.setDailyLimit(aiTutorQuotaManager.getDailyLimit());
        vo.setRemaining(aiTutorQuotaManager.getRemaining(userId));
        TbUserSubmit latest = latestFinishedSubmit(userId, questionId);
        vo.setLatestJudgeStatus(latest == null ? null : latest.getJudgeStatus());
        vo.setAccepted(latestAcceptedSubmit(userId, questionId) != null);
        vo.setAvailable(!examService.isExamOngoing(examId));
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
        if (examService.isExamOngoing(askDTO.getExamId())) {
            throw new ServiceException(ResultCode.FAILED_AI_IN_EXAM);
        }

        AiTutorChatDTO chatDTO = AiTutorConverter.toChatDTO(question, questionCaseService.listAll(questionId),
                action, content, codeFor(action, userId, questionId, askDTO.getUserCode()), submitFor(action, userId, questionId));
        Long sessionId = getOrCreateSession(userId, questionId);
        if (!aiTutorQuotaManager.tryAcquire(userId)) {
            throw new ServiceException(ResultCode.FAILED_AI_QUOTA_EXCEEDED);
        }
        chatDTO.setHistory(AiTutorConverter.toHistoryList(recentMessages(sessionId, HISTORY_MESSAGE_LIMIT)));

        String userMessage = content.isEmpty() ? action.getLabel() : content;
        return relay(userId, sessionId, action, userMessage, chatDTO);
    }

    // 发给模型的代码：优化代码思路读取已保存的草稿（前端会先自动保存），其余使用编辑器当前代码
    private String codeFor(AiTutorActionEnum action, Long userId, Long questionId, String editorCode) {
        if (action != AiTutorActionEnum.OPTIMIZE_CODE) {
            return editorCode;
        }
        String saved = codeDraftService.getSavedCode(userId, questionId);
        if (StrUtil.isBlank(saved)) {
            throw new ServiceException(ResultCode.FAILED_AI_ACTION_UNAVAILABLE, "请先编写并保存代码");
        }
        return saved;
    }

    // 快捷操作需要的提交记录：分析提交要求最近一次未通过，解释编译错误要求最近一次为编译错误，点评要求已通过
    private TbUserSubmit submitFor(AiTutorActionEnum action, Long userId, Long questionId) {
        return switch (action) {
            case ANALYZE_SUBMIT -> requireSubmit(latestFinishedSubmit(userId, questionId),
                    s -> !JudgeStatusEnum.AC.getCode().equals(s.getJudgeStatus()));
            case EXPLAIN_COMPILE -> requireSubmit(latestFinishedSubmit(userId, questionId),
                    s -> JudgeStatusEnum.CE.getCode().equals(s.getJudgeStatus()));
            case REVIEW_CODE -> requireSubmit(latestAcceptedSubmit(userId, questionId), s -> true);
            default -> null;
        };
    }

    // 把 AI 服务的流式事件转发给浏览器；正常结束时落库并返回剩余次数，失败时归还次数
    private SseEmitter relay(Long userId, Long sessionId, AiTutorActionEnum action, String userMessage, AiTutorChatDTO chatDTO) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        AiTutorStreamState state = new AiTutorStreamState();
        Disposable subscription = aiTutorClient.streamChat(chatDTO).subscribe(
                event -> onEvent(emitter, state, event),
                error -> finishWithError(emitter, userId),
                () -> onComplete(emitter, state, userId, sessionId, action, userMessage));

        // 浏览器断开或超时时停止拉取模型回复（已产生的调用照常计入次数）
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());
        emitter.onCompletion(subscription::dispose);
        return emitter;
    }

    // 处理 AI 服务的一个事件：增量文本转发给浏览器，结束事件记录到状态（错误事件已由客户端转为错误信号）
    private void onEvent(SseEmitter emitter, AiTutorStreamState state, ServerSentEvent<String> event) {
        String name = event.event();
        if (AiInternalPaths.EVENT_DELTA.equals(name)) {
            String text = JSON.parseObject(event.data()).getString(AiInternalPaths.FIELD_TEXT);
            state.getReply().append(text);
            send(emitter, AiInternalPaths.EVENT_DELTA, new JSONObject().fluentPut(AiInternalPaths.FIELD_TEXT, text));
        } else if (AiInternalPaths.EVENT_DONE.equals(name)) {
            state.setDone(JSON.parseObject(event.data()));
        }
    }

    // 流结束：正常完成则落库并告知剩余次数，否则按失败处理
    private void onComplete(SseEmitter emitter, AiTutorStreamState state, Long userId, Long sessionId,
                            AiTutorActionEnum action, String userMessage) {
        if (!state.isCompleted()) {
            finishWithError(emitter, userId);
            return;
        }
        try {
            Long messageId = saveRound(userId, sessionId, action, userMessage, state.getReply().toString(), state.getDone());
            send(emitter, AiInternalPaths.EVENT_DONE, new JSONObject()
                    .fluentPut(FIELD_MESSAGE_ID, String.valueOf(messageId))
                    .fluentPut(FIELD_REMAINING, aiTutorQuotaManager.getRemaining(userId)));
        } catch (Exception e) {
            log.error("保存 AI 辅导记录失败, userId = {}, sessionId = {}", userId, sessionId, e);
            send(emitter, AiInternalPaths.EVENT_ERROR, new JSONObject().fluentPut(AiInternalPaths.FIELD_MSG, ResultCode.ERROR.getMsg()));
        }
        emitter.complete();
    }

    // 以失败结束：归还次数并通知浏览器
    private void finishWithError(SseEmitter emitter, Long userId) {
        aiTutorQuotaManager.release(userId);
        send(emitter, AiInternalPaths.EVENT_ERROR, new JSONObject().fluentPut(AiInternalPaths.FIELD_MSG, ResultCode.FAILED_AI_BUSY.getMsg()));
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
            aiChatMessageMapper.insert(newMessage(userId, sessionId, action, AiChatRoleEnum.USER, question, now));
            TbAiChatMessage aiMsg = newMessage(userId, sessionId, action, AiChatRoleEnum.ASSISTANT, answer,
                    now.plusNanos(REPLY_TIME_OFFSET_NANOS));
            aiMsg.setModel(done.getString(AiInternalPaths.FIELD_MODEL));
            aiMsg.setPromptTokens(done.getInteger(AiInternalPaths.FIELD_PROMPT_TOKENS));
            aiMsg.setCompletionTokens(done.getInteger(AiInternalPaths.FIELD_COMPLETION_TOKENS));
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

    // 读取会话最近的若干条消息（按时间升序）
    private List<TbAiChatMessage> recentMessages(Long sessionId, int limit) {
        PageHelper.startPage(1, limit, false);
        List<TbAiChatMessage> recent = new ArrayList<>(aiChatMessageMapper.selectList(new LambdaQueryWrapper<TbAiChatMessage>()
                .eq(TbAiChatMessage::getSessionId, sessionId)
                .orderByDesc(TbAiChatMessage::getCreateTime)
                .orderByDesc(TbAiChatMessage::getMessageId)));
        Collections.reverse(recent);
        return recent;
    }

    // 获取或创建会话（写入放在编程式事务中，内部调用时注解式事务不生效；并发创建时以唯一索引兜底）
    private Long getOrCreateSession(Long userId, Long questionId) {
        TbAiChatSession session = findSession(userId, questionId);
        if (session != null) {
            return session.getSessionId();
        }
        TbAiChatSession created = new TbAiChatSession();
        created.setUserId(userId);
        created.setQuestionId(questionId);
        try {
            transactionTemplate.executeWithoutResult(status -> aiChatSessionMapper.insert(created));
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
        return firstSubmit(new LambdaQueryWrapper<TbUserSubmit>()
                .eq(TbUserSubmit::getUserId, userId)
                .eq(TbUserSubmit::getQuestionId, questionId)
                .ne(TbUserSubmit::getPass, SubmitPassEnum.JUDGING.getCode())
                .isNotNull(TbUserSubmit::getJudgeStatus)
                .orderByDesc(TbUserSubmit::getCreateTime));
    }

    // 最近一次通过的提交
    private TbUserSubmit latestAcceptedSubmit(Long userId, Long questionId) {
        return firstSubmit(new LambdaQueryWrapper<TbUserSubmit>()
                .eq(TbUserSubmit::getUserId, userId)
                .eq(TbUserSubmit::getQuestionId, questionId)
                .eq(TbUserSubmit::getJudgeStatus, JudgeStatusEnum.AC.getCode())
                .orderByDesc(TbUserSubmit::getCreateTime));
    }

    // 按条件取第一条提交记录
    private TbUserSubmit firstSubmit(LambdaQueryWrapper<TbUserSubmit> wrapper) {
        PageHelper.startPage(1, 1, false);
        List<TbUserSubmit> list = userSubmitMapper.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    // 校验提交存在且符合快捷操作要求
    private TbUserSubmit requireSubmit(TbUserSubmit submit, Predicate<TbUserSubmit> condition) {
        if (submit == null || !condition.test(submit)) {
            throw new ServiceException(ResultCode.FAILED_AI_ACTION_UNAVAILABLE);
        }
        return submit;
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
