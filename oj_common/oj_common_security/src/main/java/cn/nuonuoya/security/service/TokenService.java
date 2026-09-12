package cn.nuonuoya.security.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.common.constants.JWTConstant;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 用户登录的令牌操作
@Slf4j
@Component
public class TokenService {

    // JWT 签名密钥，从配置中心读取
    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private RedisService redisService;

    // 根据 userId 和登录用户信息生成 Token，并将用户信息写入 Redis 缓存
    public String createToken(Long userId, LoginUser loginUser) {
        // 生成随机 userKey，作为 Redis 中的唯一标识
        String userKey = UUID.fastUUID().toString();
        // 构建 JWT 载荷：存 userId 和 userKey（敏感信息不写进 JWT）
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWTConstant.LOGIN_USER_ID, userId);
        claims.put(JWTConstant.LOGIN_USER_KEY, userKey);
        String token = JwtUtils.createToken(claims, secret);
        // 以 userKey 为键将用户身份信息存入 Redis，有效期 720 分钟
        String key = getTokenKey(userKey);
        redisService.setCacheObject(key, loginUser, CacheConstants.EXPIRATION, TimeUnit.MINUTES);
        return token;
    }

    // 清洗Token 去除Bearer前缀 双引号及空白字符
    public String cleanToken(String token) {
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        token = token.trim();
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
            token = token.substring(1, token.length() - 1).trim();
        }
        while (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        return token;
    }

    // 解析并获取Token中的Claims载荷
    public Claims getClaims(String token) {
        token = cleanToken(token);
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        try {
            return JwtUtils.parseToken(token, secret);
        } catch (Exception e) {
            log.error("[Token解析] 解析令牌发生异常: {}", e.getMessage());
            return null;
        }
    }

    // 从Token中解析出用户ID
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        if (claims == null) {
            return null;
        }
        String userId = JwtUtils.getUserId(claims);
        return StrUtil.isNotEmpty(userId) ? Long.valueOf(userId) : null;
    }

    // 从Token中解析出userKey
    public String getUserKey(String token) {
        Claims claims = getClaims(token);
        return claims != null ? JwtUtils.getUserKey(claims) : null;
    }

    // 从Token中获取Redis中存储的登录用户信息
    public LoginUser getLoginUser(String token) {
        String userKey = getUserKey(token);
        if (StrUtil.isEmpty(userKey)) {
            return null;
        }
        return redisService.getCacheObject(getTokenKey(userKey), LoginUser.class);
    }

    // 删除登录用户缓存（使令牌失效）
    public void deleteLoginUser(String token) {
        String userKey = getUserKey(token);
        if (StrUtil.isNotEmpty(userKey)) {
            redisService.deleteObject(getTokenKey(userKey));
        }
    }

    // 延长Token有效期
    public void extendTokenTTL(String token) {
        Claims claims = getClaims(token);
        if (claims == null) {
            return;
        }
        String userKey = JwtUtils.getUserKey(claims);
        if (StrUtil.isEmpty(userKey)) {
            log.warn("[Token续期] 令牌中未包含有效的 userKey，跳过续期");
            return;
        }
        String key = getTokenKey(userKey);
        Long expire = redisService.getExpire(key, TimeUnit.MINUTES);
        if (expire != null && expire < CacheConstants.TOKEN_REFRESH_TIME) {
            redisService.expire(key, CacheConstants.EXPIRATION, TimeUnit.MINUTES);
            log.info("[Token续期] 用户会话即将过期，成功刷新有效期: userKey={}, 原剩余时间={}分钟", userKey, expire);
        }
    }

    // 延长Token有效期 兼容双参调用
    public void extendTokenTTL(String token, String secret) {
        extendTokenTTL(token);
    }

    private String getTokenKey(String userKey){
        return CacheConstants.LOGIN_TOKEN_KEY + userKey;
    }
}
