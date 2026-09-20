package cn.nuonuoya.friend.cache;

import cn.nuonuoya.friend.converter.UserConverter;
import cn.nuonuoya.friend.domain.TbUser;
import cn.nuonuoya.friend.mapper.UserMapper;
import cn.nuonuoya.friend.vo.UserVO;
import cn.nuonuoya.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// C端用户信息与状态缓存管理组件
@Slf4j
@Component
public class UserCacheManager {

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserMapper userMapper;

    // 默认用户信息缓存过期时长：30分钟
    private static final long USER_CACHE_TTL_MINUTES = 30;

    // 用户信息缓存键前缀（user:detail:{userId}）
    public static final String USER_CACHE_PREFIX = "user:detail:";

    // 获取用户缓存Key
    private String getUserKey(Long userId) {
        return USER_CACHE_PREFIX + userId;
    }

    // 根据用户ID获取用户视图对象（优先读Redis缓存，未命中查库并回写缓存）
    public UserVO getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = getUserKey(userId);
        UserVO userVO = redisService.getCacheObject(key, UserVO.class);
        if (userVO != null) {
            return userVO;
        }

        // 缓存未命中，查询数据库
        TbUser tbUser = userMapper.selectById(userId);
        if (tbUser == null) {
            return null;
        }

        userVO = UserConverter.toVO(tbUser);
        // 回写缓存，避免缓存穿透
        redisService.setCacheObject(key, userVO, USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return userVO;
    }

    // 主动剔除或清理指定用户缓存
    public void deleteUserCache(Long userId) {
        if (userId != null) {
            redisService.deleteObject(getUserKey(userId));
        }
    }
}
