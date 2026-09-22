package cn.nuonuoya.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.ai.config.AiProperties;
import cn.nuonuoya.ai.prompt.TutorPrompts;
import cn.nuonuoya.ai.service.AiTutorService;
import cn.nuonuoya.api.ai.constants.AiInternalPaths;
import cn.nuonuoya.api.ai.dto.AiTutorChatDTO;
import cn.nuonuoya.api.ai.dto.AiTutorHistoryDTO;
import cn.nuonuoya.api.ai.enums.AiTutorActionEnum;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// 做题辅导实现：只做模型计算，不落库；边界规则写在系统提示中
@Slf4j
@Service
public class AiTutorServiceImpl implements AiTutorService {

    @Autowired
    private ChatClient tutorChatClient;

    @Autowired
    private AiProperties aiProperties;

    // 流式对话
    @Override
    public Flux<ServerSentEvent<String>> chat(AiTutorChatDTO chatDTO) {
        AiTutorActionEnum action = AiTutorActionEnum.getByCode(chatDTO.getAction());
        if (action == null) {
            return Flux.just(errorEvent("不支持的提问类型"));
        }
        String userText = TutorPrompts.user(chatDTO, action);
        if (StrUtil.isBlank(userText)) {
            return Flux.just(errorEvent("提问内容不能为空"));
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(TutorPrompts.system(chatDTO, action)));
        for (AiTutorHistoryDTO history : CollUtil.emptyIfNull(chatDTO.getHistory())) {
            if (history == null || StrUtil.isBlank(history.getContent())) {
                continue;
            }
            messages.add(Boolean.TRUE.equals(history.getFromUser())
                    ? new UserMessage(history.getContent())
                    : new AssistantMessage(history.getContent()));
        }
        messages.add(new UserMessage(userText));

        String model = aiProperties.getTutorModel();
        long start = System.currentTimeMillis();
        AtomicReference<Usage> usage = new AtomicReference<>();
        Flux<ServerSentEvent<String>> deltas = tutorChatClient.prompt()
                .options(DashScopeChatOptions.builder()
                        .model(model)
                        .temperature(aiProperties.getTutorTemperature())
                        .maxToken(aiProperties.getTutorMaxTokens())
                        .incrementalOutput(true)
                        .build())
                .messages(messages)
                .stream()
                .chatResponse()
                .timeout(Duration.ofSeconds(aiProperties.getTutorTimeoutSeconds()))
                .doOnNext(response -> rememberUsage(response, usage))
                .map(this::textOf)
                .filter(StrUtil::isNotEmpty)
                .map(text -> ServerSentEvent.builder(new JSONObject().fluentPut("text", text).toJSONString())
                        .event(AiInternalPaths.EVENT_DELTA).build());

        return deltas
                .concatWith(Flux.defer(() -> {
                    log.info("AI 辅导完成, model = {}, action = {}, 耗时 = {} ms", model, action, System.currentTimeMillis() - start);
                    return Flux.just(doneEvent(model, usage.get()));
                }))
                .onErrorResume(e -> {
                    log.warn("AI 辅导失败, model = {}, action = {}, 耗时 = {} ms, error = {}", model, action,
                            System.currentTimeMillis() - start, e.getMessage());
                    return Flux.just(errorEvent("AI 服务繁忙，请稍后重试"));
                });
    }

    // 记录最后一次出现的有效用量（流式输出只在末尾携带用量）
    private void rememberUsage(ChatResponse response, AtomicReference<Usage> usage) {
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null
                && response.getMetadata().getUsage().getTotalTokens() != null
                && response.getMetadata().getUsage().getTotalTokens() > 0) {
            usage.set(response.getMetadata().getUsage());
        }
    }

    // 取出一段增量文本
    private String textOf(ChatResponse response) {
        if (response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return StrUtil.nullToEmpty(response.getResult().getOutput().getText());
    }

    // 结束事件：模型名与用量
    private ServerSentEvent<String> doneEvent(String model, Usage usage) {
        JSONObject data = new JSONObject().fluentPut("model", model);
        if (usage != null) {
            data.put("promptTokens", usage.getPromptTokens());
            data.put("completionTokens", usage.getCompletionTokens());
        }
        return ServerSentEvent.builder(data.toJSONString()).event(AiInternalPaths.EVENT_DONE).build();
    }

    // 错误事件
    private ServerSentEvent<String> errorEvent(String message) {
        return ServerSentEvent.builder(new JSONObject().fluentPut("msg", message).toJSONString())
                .event(AiInternalPaths.EVENT_ERROR).build();
    }
}
