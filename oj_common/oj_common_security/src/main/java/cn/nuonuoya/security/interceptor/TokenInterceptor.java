package cn.nuonuoya.security.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// token的拦截器
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = getToken(request);
        if (StrUtil.isNotEmpty(token)) {
            tokenService.extendTokenTTL(token);
        }
        return true;
    }

    // 从请求中获取token，容错处理引号、多余Bearer前缀及空白字符
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(HttpConstants.AUTHENTICATION);
        if (StrUtil.isEmpty(token)) {
            token = request.getHeader("token");
        }
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        token = token.trim();
        // 去除可能的双引号包裹（从JSON复制时易带入引号）
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
            token = token.substring(1, token.length() - 1).trim();
        }
        // 循环去除可能重复的 Bearer 前缀
        while (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        return token;
    }
}
