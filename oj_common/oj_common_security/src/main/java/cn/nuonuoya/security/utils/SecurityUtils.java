package cn.nuonuoya.security.utils;

import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.utils.ThreadLocalUtil;

// 当前登录用户上下文工具类（由 TokenInterceptor 在请求入口写入）
public class SecurityUtils {

    // 获取当前登录用户ID，未登录返回 null
    public static Long getUserId() {
        return ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
    }

    // 获取当前登录会话标识，未登录返回 null
    public static String getUserKey() {
        return ThreadLocalUtil.get(HttpConstants.USER_KEY);
    }
}
