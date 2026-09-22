package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.api.AiModerationInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// AI 审核接口 Feign 客户端
@FeignClient(name = "oj-ai", contextId = "aiModerationFeignClient", configuration = AiModerationFeignConfig.class)
public interface AiModerationFeignClient extends AiModerationInternalApi {
}
