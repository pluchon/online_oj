package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.api.AiAppealInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// 申诉 AI 初审 Feign 客户端（AI 调用耗时长，使用单独的超时配置）
@FeignClient(name = "oj-ai", contextId = "aiAppealFeignClient", configuration = AiAppealFeignConfig.class)
public interface AiAppealFeignClient extends AiAppealInternalApi {
}
