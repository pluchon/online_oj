package cn.nuonuoya.job.client;

import cn.nuonuoya.api.system.api.SystemQuestionInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// B端题目内部接口 Feign 客户端
@FeignClient(name = "oj-system", contextId = "jobSystemQuestionFeignClient")
public interface SystemQuestionFeignClient extends SystemQuestionInternalApi {
}
