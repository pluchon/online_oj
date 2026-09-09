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

    // 成功响应无数据
    public static <T> OJResult<T> ok() {
        return assembleResult(null, ResultCode.SUCCESS);
    }

    // 成功响应带数据
    public static <T> OJResult<T> ok(T data) {
        return assembleResult(data, ResultCode.SUCCESS);
    }

    // 默认失败响应
    public static <T> OJResult<T> fail() {
        return assembleResult(null, ResultCode.FAILED);
    }

    // 指定错误码失败响应
    public static <T> OJResult<T> fail(ResultCode resultCode) {
        return assembleResult(null, resultCode);
    }

    // 统一装配结果对象
    private static <T> OJResult<T> assembleResult(T data, ResultCode resultCode) {
        OJResult<T> r = new OJResult<>();
        r.setCode(resultCode.getCode());
        r.setData(data);
        r.setMsg(resultCode.getMsg());
        return r;
    }
}
