package cn.nuonuoya.security.exception;

import cn.nuonuoya.common.enums.ResultCode;
import lombok.Getter;

// 业务异常（携带统一错误码，由全局异常处理器转换为响应）
@Getter
public class ServiceException extends RuntimeException {

    // 业务错误码
    private final ResultCode resultCode;

    public ServiceException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }
}
