package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.judge.api.JudgeInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// 判题服务内部接口 Feign 客户端
@FeignClient(name = "oj-judge", contextId = "judgeFeignClient")
public interface JudgeFeignClient extends JudgeInternalApi {
}
