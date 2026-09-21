package cn.nuonuoya.security.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.common.constants.JWTConstant;
import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.common.utils.JwtUtils;
import cn.nuonuoya.redis.service.RedisService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 登录令牌服务（JWT 只存 userId 与 userKey，会话信息存 Redis）
@Slf4j
@Component
public class TokenService {

    // JWT 签名密钥，从配置中心读取
    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private RedisService redisService;

    // 签发令牌，并以随机 userKey 为键将登录信息写入 Redis
    public String createToken(Long userId, LoginUser loginUser) {
        String userKey = UUID.fastUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWTConstant.LOGIN_USER_ID, userId);
        claims.put(JWTConstant.LOGIN_USER_KEY, userKey);
        String token = JwtUtils.createToken(claims, secret);
        redisService.setCacheObject(getTokenKey(userKey), loginUser, CacheConstants.EXPIRATION, TimeUnit.MINUTES);
        return token;
    }

    // 解析令牌载荷，令牌为空或无效时返回 null
    public Claims getClaims(String token) {
        token = JwtUtils.cleanToken(token);
        if (token == null) {
            return null;
        }
        try {
            return JwtUtils.parseToken(token, secret);
        } catch (Exception e) {
            log.warn("[Token解析] 令牌无效: {}", e.getMessage());
            return null;
        }
    }

    // 从令牌中解析用户ID
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims == null ? null : getUserId(claims);
    }

    // 从载荷中获取用户ID
    public Long getUserId(Claims claims) {
        String userId = JwtUtils.getUserId(claims);
        return StrUtil.isNotEmpty(userId) ? Long.valueOf(userId) : null;
    }

    // 从令牌中解析 userKey
    public String getUserKey(String token) {
        Claims claims = getClaims(token);
        return claims != null ? JwtUtils.getUserKey(claims) : null;
    }

    // 获取会话对应的登录用户信息，会话不存在返回 null
    public LoginUser getLoginUserByKey(String userKey) {
        if (StrUtil.isEmpty(userKey)) {
            return null;
        }
        return redisService.getCacheObject(getTokenKey(userKey), LoginUser.class);
    }

    // 更新会话中的登录用户信息（保留剩余有效期）
    public void updateLoginUser(String userKey, LoginUser loginUser) {
        if (StrUtil.isEmpty(userKey) || loginUser == null) {
            return;
        }
        String key = getTokenKey(userKey);
        Long expire = redisService.getExpire(key, TimeUnit.MINUTES);
        long ttl = expire != null && expire > 0 ? expire : CacheConstants.EXPIRATION;
        redisService.setCacheObject(key, loginUser, ttl, TimeUnit.MINUTES);
    }

    // 删除会话（使令牌失效）
    public void deleteLoginUserByKey(String userKey) {
        if (StrUtil.isNotEmpty(userKey)) {
            redisService.deleteObject(getTokenKey(userKey));
        }
    }

    // 会话剩余有效期低于阈值时自动续期
    public void extendTokenTTL(Claims claims) {
        String userKey = JwtUtils.getUserKey(claims);
        if (StrUtil.isEmpty(userKey)) {
            return;
        }
        String key = getTokenKey(userKey);
        Long expire = redisService.getExpire(key, TimeUnit.MINUTES);
        if (expire != null && expire > 0 && expire < CacheConstants.TOKEN_REFRESH_TIME) {
            redisService.expire(key, CacheConstants.EXPIRATION, TimeUnit.MINUTES);
            log.debug("[Token续期] 会话即将过期，已刷新有效期，原剩余 {} 分钟", expire);
        }
    }

    // 拼接登录会话缓存键
    private String getTokenKey(String userKey) {
        return CacheConstants.LOGIN_TOKEN_KEY + userKey;
    }
}
