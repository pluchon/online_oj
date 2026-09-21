package cn.nuonuoya.security.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.utils.JwtUtils;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import cn.nuonuoya.security.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 登录上下文拦截器：解析当前用户写入线程上下文并自动续期（不做拦截，鉴权由网关负责）
@Component
public class TokenInterceptor implements HandlerInterceptor {

    // 兼容旧客户端的令牌请求头
    private static final String LEGACY_TOKEN_HEADER = "token";

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 优先使用网关透传的用户身份
        String userId = request.getHeader(HttpConstants.USER_ID);
        String userKey = request.getHeader(HttpConstants.USER_KEY);

        Claims claims = tokenService.getClaims(getToken(request));
        if (claims != null) {
            tokenService.extendTokenTTL(claims);
            // 未经网关（如本地直连调试）时从令牌解析
            if (StrUtil.isEmpty(userId)) {
                Long parsedUserId = tokenService.getUserId(claims);
                userId = parsedUserId == null ? null : String.valueOf(parsedUserId);
            }
            if (StrUtil.isEmpty(userKey)) {
                userKey = JwtUtils.getUserKey(claims);
            }
        }
        if (StrUtil.isNotEmpty(userId)) {
            ThreadLocalUtil.set(HttpConstants.USER_ID, userId);
        }
        if (StrUtil.isNotEmpty(userKey)) {
            ThreadLocalUtil.set(HttpConstants.USER_KEY, userKey);
        }
        return true;
    }

    // 请求结束后清理线程上下文，防止线程复用导致身份串号
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ThreadLocalUtil.remove();
    }

    // 从请求头获取令牌
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(HttpConstants.AUTHENTICATION);
        return StrUtil.isEmpty(token) ? request.getHeader(LEGACY_TOKEN_HEADER) : token;
    }
}
