package cn.nuonuoya.security.exception;

import cn.nuonuoya.common.enums.ResultCode;
import lombok.Getter;

// 业务异常（携带统一错误码，由全局异常处理器转换为响应）
@Getter
public class ServiceException extends RuntimeException {

    // 业务错误码
    private final ResultCode resultCode;

    // 使用错误码的默认提示
    public ServiceException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    // 使用自定义提示（如需在提示中带上动态数值）
    public ServiceException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
