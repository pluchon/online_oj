package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.api.FriendStatsInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// C端统计内部接口 Feign 客户端
@FeignClient(name = "oj-friend", contextId = "friendStatsFeignClient")
public interface FriendStatsFeignClient extends FriendStatsInternalApi {
}
