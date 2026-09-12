package cn.nuonuoya.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// 全局异常处理器，不同的异常可以通过不同的方法进行处理
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 请求方式不支持异常处理
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public OJResult<?> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return OJResult.fail(ResultCode.ERROR);
    }

    // 运行时异常处理
    @ExceptionHandler(RuntimeException.class)
    public OJResult<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',运行时发生异常.", requestURI, e);
        return OJResult.fail(ResultCode.ERROR);
    }

    // 自定义异常
    @ExceptionHandler(ServiceException.class)
    public OJResult<?> handleServiceException(ServiceException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        ResultCode resultCode = e.getResultCode();
        log.error("请求地址'{}',业务发生异常.", requestURI, e);
        return OJResult.fail(resultCode);
    }

    // 参数校验相关的异常
    @ExceptionHandler(BindException.class)
    public OJResult<Void> handleBindException(BindException e) {
        log.error(e.getMessage());
        String message = join(e.getAllErrors(), DefaultMessageSourceResolvable::getDefaultMessage, ", ");
        return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE.getCode(), message);
    }

    private <E> String join(Collection<E> collection, Function<E, String> function, CharSequence delimiter) {
        if (CollUtil.isEmpty(collection)) {
            return StrUtil.EMPTY;
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
    }

    // 系统异常兜底处理
    @ExceptionHandler(Exception.class)
    public OJResult<?> handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return OJResult.fail(ResultCode.ERROR);
    }
}