package cn.nuonuoya.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.JWTConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Map;

// JWT 令牌工具类
public class JwtUtils {

    // 令牌类型前缀
    private static final String BEARER_PREFIX = "bearer ";

    // 清洗令牌：去除首尾空白、双引号包裹与 Bearer 前缀，空值返回 null
    public static String cleanToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        token = token.trim();
        if (token.length() > 1 && token.startsWith("\"") && token.endsWith("\"")) {
            token = token.substring(1, token.length() - 1).trim();
        }
        while (token.toLowerCase().startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return StrUtil.isEmpty(token) ? null : token;
    }

    // 使用密钥签发令牌
    public static String createToken(Map<String, Object> claims, String secret) {
        return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    // 校验并解析令牌载荷
    public static Claims parseToken(String token, String secret) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    // 从载荷中获取 userKey（Redis 查询用）
    public static String getUserKey(Claims claims) {
        Object val = claims.get(JWTConstant.LOGIN_USER_KEY);
        return val != null ? String.valueOf(val) : null;
    }

    // 从载荷中获取 userId
    public static String getUserId(Claims claims) {
        Object val = claims.get(JWTConstant.LOGIN_USER_ID);
        return val != null ? String.valueOf(val) : null;
    }
}
