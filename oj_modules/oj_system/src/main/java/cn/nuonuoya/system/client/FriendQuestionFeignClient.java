package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.api.FriendQuestionInternalApi;
import org.springframework.cloud.openfeign.FeignClient;

// C端题目内部接口 Feign 客户端
@FeignClient(name = "oj-friend", contextId = "friendQuestionFeignClient")
public interface FriendQuestionFeignClient extends FriendQuestionInternalApi {
}
