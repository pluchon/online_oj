package cn.nuonuoya.system.client;

import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;
import java.util.function.Supplier;

// 远程调用保护：远程异常，或关键字段为空的结果，都按指定错误码抛出
// （提供方出错时全局异常处理仍以 200 返回错误码，Feign 会把它解析成字段全空的对象，不能当作正常结果）
@Slf4j
public final class RemoteCallGuard {

    private RemoteCallGuard() {
    }

    // 执行远程调用并校验结果，失败时抛出 failCode
    public static <T> T call(String action, ResultCode failCode, Supplier<T> supplier, Predicate<T> valid) {
        T result;
        try {
            result = supplier.get();
        } catch (Exception e) {
            log.error("{}失败, error = {}", action, e.getMessage());
            throw new ServiceException(failCode);
        }
        if (!valid.test(result)) {
            log.error("{}失败：提供方返回了错误响应", action);
            throw new ServiceException(failCode);
        }
        return result;
    }
}
