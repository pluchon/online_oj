package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.api.FriendExamInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// C端竞赛内部接口 Feign 客户端
@FeignClient(name = "oj-friend", contextId = "friendExamFeignClient")
public interface FriendExamFeignClient extends FriendExamInternalApi {
}
