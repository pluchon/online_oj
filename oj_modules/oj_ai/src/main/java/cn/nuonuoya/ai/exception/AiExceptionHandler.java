package cn.nuonuoya.ai.exception;

import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.enums.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// AI 内部接口异常处理：失败一律返回非 2xx 状态码，避免调用方把错误体当作正常结果解析
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "cn.nuonuoya.ai")
public class AiExceptionHandler {

    // 模型调用失败
    @ExceptionHandler(AiModelException.class)
    public ResponseEntity<OJResult<Void>> handleModelException(AiModelException e, HttpServletRequest request) {
        log.warn("请求地址'{}'模型调用失败: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(OJResult.fail(ResultCode.FAILED_AI_BUSY));
    }

    // 请求参数校验失败或请求体格式错误
    @ExceptionHandler({BindException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<OJResult<Void>> handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("请求地址'{}'参数错误: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE));
    }

    // 未知异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<OJResult<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("请求地址'{}'发生未知异常", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OJResult.fail(ResultCode.ERROR));
    }
}
