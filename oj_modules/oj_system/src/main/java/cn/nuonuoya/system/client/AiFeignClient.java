package cn.nuonuoya.system.client;

import cn.nuonuoya.api.ai.api.AiInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// AI 服务内部接口 Feign 客户端（模型调用较慢，使用单独的超时配置）
@FeignClient(name = "oj-ai", contextId = "aiFeignClient", configuration = AiFeignConfig.class)
public interface AiFeignClient extends AiInternalApi {
}
