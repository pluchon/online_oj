package cn.nuonuoya.ai.controller;

import cn.nuonuoya.ai.service.AiTutorService;
import cn.nuonuoya.api.ai.constants.AiInternalPaths;
import cn.nuonuoya.api.ai.dto.AiTutorChatDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

// 做题辅导内部接口（流式，不经网关暴露，由 friend 用 WebClient 调用）
@RestController
public class AiTutorController {

    @Autowired
    private AiTutorService aiTutorService;

    /** 做题辅导流式对话 */
    @PostMapping(value = AiInternalPaths.TUTOR_CHAT, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody AiTutorChatDTO chatDTO) {
        return aiTutorService.chat(chatDTO);
    }
}
