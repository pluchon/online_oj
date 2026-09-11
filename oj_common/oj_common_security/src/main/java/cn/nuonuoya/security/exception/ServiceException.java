package cn.nuonuoya.security.exception;

import cn.nuonuoya.common.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 自定义异常
@AllArgsConstructor
@Getter
public class ServiceException extends RuntimeException {

    private ResultCode resultCode;

}
