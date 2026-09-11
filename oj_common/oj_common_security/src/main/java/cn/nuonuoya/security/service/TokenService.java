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

    // 延长token的有效期，也就是延长Redis的有效期，必须是身份认证通过后，到达controller之前
    // 最后决定放在拦截器里面，因为拦截器是在所有的过滤器后执行的
    public void extendTokenTTL(String token, String secret) {
        Claims claims;
        try {
            claims = JwtUtils.parseToken(token, secret);
            // 获取令牌中信息 解析payload中信息
            if (claims == null) {
                log.warn("[Token续期] 解析出的Claims为空，跳过续期");
                return;
            }
        } catch (Exception e) {
            log.error("[Token续期] 解析令牌发生异常，跳过续期: {}", e.getMessage());
            return;
        }
        // 校验通过
        String userKey = JwtUtils.getUserKey(claims);
        if (StrUtil.isEmpty(userKey)) {
            log.warn("[Token续期] 令牌中未包含有效的 userKey，跳过续期");
            return;
        }
        // 延长有效期
        String key = getTokenKey(userKey);
        // 校验剩余时间，决定是不是要进行延长
        Long expire = redisService.getExpire(key, TimeUnit.MINUTES);
        if (expire != null && expire < CacheConstants.TOKEN_REFRESH_TIME) {
            // 刷新恢复为满血有效期（720分钟）
            redisService.expire(key, CacheConstants.EXPIRATION, TimeUnit.MINUTES);
            log.info("[Token续期] 用户会话即将过期，成功刷新有效期: userKey={}, 原剩余时间={}分钟", userKey, expire);
        }
    }

    // 延长token有效期（单参数重载，直接使用自身注入的 secret）
    public void extendTokenTTL(String token) {
        extendTokenTTL(token, this.secret);
    }

    private String getTokenKey(String userKey){
        return CacheConstants.LOGIN_TOKEN_KEY + userKey;
    }
}
