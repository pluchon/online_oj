package cn.nuonuoya.security.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
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
        // 优先从网关透传的请求头中获取用户ID与UserKey
        String userId = request.getHeader(HttpConstants.USER_ID);
        String userKey = request.getHeader(HttpConstants.USER_KEY);
        String token = getToken(request);
        if (StrUtil.isNotEmpty(token)) {
            tokenService.extendTokenTTL(token);
            // 若网关未透传（如本地直连调试），则兜底从Token解析
            if (StrUtil.isEmpty(userId)) {
                Long parsedUserId = tokenService.getUserId(token);
                if (parsedUserId != null) {
                    userId = String.valueOf(parsedUserId);
                }
            }
            if (StrUtil.isEmpty(userKey)) {
                userKey = tokenService.getUserKey(token);
            }
        }
        // 绑定到下游微服务的当前线程上下文
        if (StrUtil.isNotEmpty(userId)) {
            ThreadLocalUtil.set(HttpConstants.USER_ID, userId);
        }
        if (StrUtil.isNotEmpty(userKey)) {
            ThreadLocalUtil.set(HttpConstants.USER_KEY, userKey);
        }
        return true;
    }

    // 请求处理完毕后强制清理上下文，防止线程池污染
    // 非常重要！！
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
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
