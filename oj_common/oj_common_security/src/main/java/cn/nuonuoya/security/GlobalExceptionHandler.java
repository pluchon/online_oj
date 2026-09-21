package cn.nuonuoya.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;
import java.util.stream.Collectors;

// 全局异常处理器（业务与参数错误记 warn，未知异常记 error 并隐藏细节）
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 请求方式不支持
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public OJResult<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求地址'{}'不支持'{}'请求", request.getRequestURI(), e.getMethod());
        return OJResult.fail(ResultCode.FAILED.getCode(), "不支持的请求方式");
    }

    // 业务异常
    @ExceptionHandler(ServiceException.class)
    public OJResult<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("请求地址'{}'业务异常: {}", request.getRequestURI(), e.getMessage());
        return OJResult.fail(e.getResultCode());
    }

    // 请求体或表单参数校验失败
    @ExceptionHandler(BindException.class)
    public OJResult<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        log.warn("请求地址'{}'参数校验失败: {}", request.getRequestURI(), message);
        return paramsInvalid(message);
    }

    // 方法参数约束校验失败（@Validated 标注的简单参数）
    @ExceptionHandler(ConstraintViolationException.class)
    public OJResult<Void> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        String message = CollUtil.isEmpty(e.getConstraintViolations()) ? null : e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("请求地址'{}'参数校验失败: {}", request.getRequestURI(), message);
        return paramsInvalid(message);
    }

    // 缺少必填参数、参数类型不匹配、请求体格式错误
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public OJResult<Void> handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("请求地址'{}'参数错误: {}", request.getRequestURI(), e.getMessage());
        return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE);
    }

    // 未知异常兜底
    @ExceptionHandler(Exception.class)
    public OJResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("请求地址'{}'发生未知异常", request.getRequestURI(), e);
        return OJResult.fail(ResultCode.ERROR);
    }

    // 组装参数校验失败响应（无具体信息时使用默认提示）
    private OJResult<Void> paramsInvalid(String message) {
        if (StrUtil.isBlank(message)) {
            return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE.getCode(), message);
    }
}
