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

    // 从请求中获取token并清洗
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(HttpConstants.AUTHENTICATION);
        if (StrUtil.isEmpty(token)) {
            token = request.getHeader("token");
        }
        return tokenService.cleanToken(token);
    }
}
