package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.constants.AiInternalPaths;
import cn.nuonuoya.api.ai.dto.AiTutorChatDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

// AI 辅导流式调用边界（Feign 不支持流式响应，改用负载均衡的 WebClient）
@Slf4j
@Component
public class AiTutorClient {

    // 服务端事件的反序列化类型
    private static final ParameterizedTypeReference<ServerSentEvent<String>> EVENT_TYPE = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private WebClient.Builder loadBalancedWebClientBuilder;

    // 整个流的最长耗时（秒），可在 Nacos 中覆盖
    @Value("${oj.ai.tutor.stream-timeout-seconds:120}")
    private long streamTimeoutSeconds;

    // 流式对话；连接失败、超时与非 2xx 均以错误信号结束，由调用方转换为错误事件
    public Flux<ServerSentEvent<String>> streamChat(AiTutorChatDTO chatDTO) {
        return loadBalancedWebClientBuilder.build()
                .post()
                .uri("http://" + AiInternalPaths.SERVICE_NAME + AiInternalPaths.TUTOR_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(chatDTO)
                .retrieve()
                .bodyToFlux(EVENT_TYPE)
                .timeout(Duration.ofSeconds(streamTimeoutSeconds))
                .doOnError(e -> log.error("调用 AI 辅导失败, error = {}", e.getMessage()));
    }
}
