package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.constants.AiInternalPaths;
import cn.nuonuoya.api.ai.dto.AiTutorChatDTO;
import cn.nuonuoya.friend.constants.SentinelResources;
import com.alibaba.csp.sentinel.adapter.reactor.SentinelReactorTransformer;
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

// AI 辅导流式调用边界（Feign 不支持流式响应，改用负载均衡的 WebClient；整个流纳入 Sentinel 资源，被限流或熔断时以 BlockException 错误信号结束）
@Slf4j
@Component
public class AiTutorClient {

    // 负载均衡地址前缀（按服务名解析实例）
    private static final String LOAD_BALANCED_SCHEME = "http://";

    // 服务端事件的反序列化类型
    private static final ParameterizedTypeReference<ServerSentEvent<String>> EVENT_TYPE = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private WebClient.Builder loadBalancedWebClientBuilder;

    // 整个流的最长耗时（秒），可在 Nacos 中覆盖
    @Value("${oj.ai.tutor.stream-timeout-seconds:120}")
    private long streamTimeoutSeconds;

    // 流式对话；连接失败、超时、非 2xx、AI 返回错误事件、被限流或熔断均以错误信号结束，由调用方转换为错误事件并归还次数
    public Flux<ServerSentEvent<String>> streamChat(AiTutorChatDTO chatDTO) {
        return loadBalancedWebClientBuilder.build()
                .post()
                .uri(LOAD_BALANCED_SCHEME + AiInternalPaths.SERVICE_NAME + AiInternalPaths.TUTOR_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(chatDTO)
                .retrieve()
                .bodyToFlux(EVENT_TYPE)
                .timeout(Duration.ofSeconds(streamTimeoutSeconds))
                .concatMap(event -> AiInternalPaths.EVENT_ERROR.equals(event.event())
                        ? Flux.<ServerSentEvent<String>>error(new IllegalStateException("AI 辅导返回错误事件"))
                        : Flux.just(event))
                .transform(new SentinelReactorTransformer<>(SentinelResources.AI_TUTOR))
                .doOnError(e -> log.error("调用 AI 辅导失败, error = {}", e.getClass().getSimpleName() + ": " + e.getMessage()));
    }
}
