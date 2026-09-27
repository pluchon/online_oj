package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.api.AiReviewInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// 赛后复盘 Feign 客户端（AI 调用耗时长，使用单独的超时配置）
@FeignClient(name = "oj-ai", contextId = "aiReviewFeignClient", configuration = AiReviewFeignConfig.class)
public interface AiReviewFeignClient extends AiReviewInternalApi {
}
