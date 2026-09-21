package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.api.FriendUserInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// C端用户内部接口 Feign 客户端
@FeignClient(name = "oj-friend", contextId = "friendUserFeignClient")
public interface FriendUserFeignClient extends FriendUserInternalApi {
}
