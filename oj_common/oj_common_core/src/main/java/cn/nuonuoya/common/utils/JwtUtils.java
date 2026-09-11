package cn.nuonuoya.common.utils;

import cn.nuonuoya.common.constants.JWTConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Map;

public class JwtUtils {

    /**
     * ⽣成令牌
     *
     * @param claims 数据
     * @param secret 密钥
     * @return 令牌
     */
    public static String createToken(Map<String, Object> claims, String secret) {
        return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    /**
     * 从令牌中获取数据
     *
     * @param token 令牌
     * @param secret 密钥
     * @return 数据
     */
    public static Claims parseToken(String token, String secret) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    // 从 Claims 中获取 userKey（Redis 查询用）
    public static String getUserKey(Claims claims) {
        Object val = claims.get(JWTConstant.LOGIN_USER_KEY);
        return val != null ? String.valueOf(val) : null;
    }

    // 从 Claims 中获取 userId
    public static String getUserId(Claims claims) {
        Object val = claims.get(JWTConstant.LOGIN_USER_ID);
        return val != null ? String.valueOf(val) : null;
    }
}
