package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.api.FriendSubmitInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// C端提交记录内部接口 Feign 客户端
@FeignClient(name = "oj-friend", contextId = "friendSubmitFeignClient")
public interface FriendSubmitFeignClient extends FriendSubmitInternalApi {
}
