package cn.nuonuoya.friend.aspect;

import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.cache.UserCacheManager;
import cn.nuonuoya.friend.enums.UserStatusEnum;
import cn.nuonuoya.friend.vo.UserVO;
import cn.nuonuoya.security.utils.SecurityUtils;
import cn.nuonuoya.security.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 用户状态检查切面（防重复代码，统一拦截封禁用户）
@Slf4j
@Aspect
@Component
public class UserStatusCheckAspect {

    @Autowired
    private UserCacheManager userCacheManager;

    // 在受保护操作执行前拦截校验用户账号状态
    @Before("@annotation(checkUserStatus)")
    public void before(JoinPoint point, CheckUserStatus checkUserStatus) {
        Long userId = SecurityUtils.getUserId();
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
