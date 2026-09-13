package cn.nuonuoya.common.domain;

import cn.nuonuoya.common.enums.ResultCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 表格列表统一结果响应类
@Getter
@Setter
@NoArgsConstructor
public class TableDataResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 总记录数
    private long total;

    // 列表数据
    private List<T> rows;

    // 消息状态码
    private int code;

    // 消息内容
    private String msg;

    // 未查出任何数据时调用
    public static <T> TableDataResult<T> empty() {
        TableDataResult<T> rspData = new TableDataResult<>();
        rspData.setCode(ResultCode.SUCCESS.getCode());
        rspData.setRows(new ArrayList<>());
        rspData.setMsg(ResultCode.SUCCESS.getMsg());
        rspData.setTotal(0);
        return rspData;
    }

    // 查出数据时调用
    public static <T> TableDataResult<T> success(List<T> list, long total) {
        TableDataResult<T> rspData = new TableDataResult<>();
        rspData.setCode(ResultCode.SUCCESS.getCode());
        rspData.setRows(list != null ? list : Collections.emptyList());
        rspData.setMsg(ResultCode.SUCCESS.getMsg());
        rspData.setTotal(total);
        return rspData;
    }

    // 发生业务失败时调用
    public static <T> TableDataResult<T> fail(ResultCode resultCode) {
        TableDataResult<T> rspData = new TableDataResult<>();
        rspData.setCode(resultCode.getCode());
        rspData.setMsg(resultCode.getMsg());
        rspData.setTotal(0);
        rspData.setRows(Collections.emptyList());
        return rspData;
    }
}
