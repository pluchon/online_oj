package cn.nuonuoya.friend.aspect;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import cn.nuonuoya.friend.cache.UserCacheManager;
import cn.nuonuoya.friend.enums.UserStatusEnum;
import cn.nuonuoya.friend.vo.UserVO;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// 用户状态检查切面（防重复代码，统一拦截封禁用户）
@Slf4j
@Aspect
@Component
public class UserStatusCheckAspect {

    @Autowired
    private UserCacheManager userCacheManager;

    @Autowired
    private TokenService tokenService;

    // 从线程上下文或请求头安全提取当前登录用户ID
    private Long getCurrentUserId() {
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (userId != null) {
            return userId;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String headerUserId = request.getHeader(HttpConstants.USER_ID);
            if (StrUtil.isNotBlank(headerUserId)) {
                return Convert.toLong(headerUserId);
            }
            String token = request.getHeader(HttpConstants.AUTHENTICATION);
            if (StrUtil.isNotBlank(token)) {
                return tokenService.getUserId(tokenService.cleanToken(token));
            }
        }
        return null;
    }

    // 在受保护操作执行前拦截校验用户账号状态
    @Before("@annotation(checkUserStatus)")
    public void before(JoinPoint point, CheckUserStatus checkUserStatus) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        UserVO user = userCacheManager.getUserById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        if (UserStatusEnum.BANNED.getCode().equals(user.getStatus())) {
            log.warn("拦截被拉黑封禁用户操作: userId = {}, targetMethod = {}", userId, point.getSignature().getName());
            throw new ServiceException(ResultCode.FAILED_USER_BANNED);
        }
    }
}
