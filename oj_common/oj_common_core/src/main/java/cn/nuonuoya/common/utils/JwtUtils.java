package cn.nuonuoya.common.utils;

import cn.nuonuoya.common.constants.JWTConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Map;

// JWT 令牌工具类
public class JwtUtils {

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
