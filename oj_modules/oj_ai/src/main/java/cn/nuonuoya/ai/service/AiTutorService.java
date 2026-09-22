package cn.nuonuoya.ai.service;

import cn.nuonuoya.api.ai.dto.AiTutorChatDTO;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

// 做题辅导 AI 能力
public interface AiTutorService {

    // 流式对话：先逐段返回增量文本，结束时返回模型与用量，失败时返回错误事件
    Flux<ServerSentEvent<String>> chat(AiTutorChatDTO chatDTO);
}
