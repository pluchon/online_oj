package cn.nuonuoya.common.domain;

import cn.nuonuoya.common.enums.ResultCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

// 统一响应
@Getter
@Setter
public class OJResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 状态码
    private int code;
    // 提示信息
    private String msg;
    // 响应数据
    private T data;

    public OJResult() {
    }

    public OJResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ======================== ok 方法组 ========================

    // 成功，无数据
    public static <T> OJResult<T> ok() {
        return assembleResult(null, ResultCode.SUCCESS);
    }

    // 成功，带数据
    public static <T> OJResult<T> ok(T data) {
        return assembleResult(data, ResultCode.SUCCESS);
    }

    // 成功，自定义提示信息并带数据
    public static <T> OJResult<T> ok(String msg, T data) {
        return assembleResult(ResultCode.SUCCESS.getCode(), msg, data);
    }

    // ======================== fail 方法组 ========================

    // 默认失败
    public static <T> OJResult<T> fail() {
        return assembleResult(null, ResultCode.FAILED);
    }

    // 指定错误码失败
    public static <T> OJResult<T> fail(ResultCode resultCode) {
        return assembleResult(null, resultCode);
    }

    // 自定义错误码和提示信息
    public static <T> OJResult<T> fail(int code, String msg) {
        return assembleResult(code, msg, null);
    }

    // 默认失败码，自定义提示信息
    public static <T> OJResult<T> fail(String msg) {
        return assembleResult(ResultCode.FAILED.getCode(), msg, null);
    }

    // ======================== 私有装配方法 ========================

    // 通过枚举装配
    private static <T> OJResult<T> assembleResult(T data, ResultCode resultCode) {
        OJResult<T> r = new OJResult<>();
        r.setCode(resultCode.getCode());
        r.setMsg(resultCode.getMsg());
        r.setData(data);
        return r;
    }

    // 通过自定义 code 和 msg 装配
    private static <T> OJResult<T> assembleResult(int code, String msg, T data) {
        OJResult<T> r = new OJResult<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
