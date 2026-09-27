package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.api.FriendAppealInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// C端申诉内部接口 Feign 客户端
@FeignClient(name = "oj-friend", contextId = "friendAppealFeignClient")
public interface FriendAppealFeignClient extends FriendAppealInternalApi {
}
