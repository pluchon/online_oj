package cn.nuonuoya.friend.controller;

import cn.nuonuoya.api.friend.api.FriendUserInternalApi;
import cn.nuonuoya.friend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

// C端用户内部接口控制器（网关已屏蔽 internal 路径，仅供服务间调用）
@RestController
public class UserInternalController implements FriendUserInternalApi {

    @Autowired
    private UserService userService;

    /** 清除指定用户的详情缓存 */
    @Override
    public Boolean evictUserCache(Long userId) {
        userService.evictUserCache(userId);
        return Boolean.TRUE;
    }
}
