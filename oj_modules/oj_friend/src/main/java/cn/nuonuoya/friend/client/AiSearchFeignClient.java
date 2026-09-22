package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.api.AiSearchInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// AI 向量接口 Feign 客户端
@FeignClient(name = "oj-ai", contextId = "aiSearchFeignClient", configuration = AiSearchFeignConfig.class)
public interface AiSearchFeignClient extends AiSearchInternalApi {
}
