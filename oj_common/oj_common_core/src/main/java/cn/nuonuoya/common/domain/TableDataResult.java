package cn.nuonuoya.common.domain;

import cn.nuonuoya.common.enums.ResultCode;
import lombok.AllArgsConstructor;
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

    // 数据载体（兼容包装层结构）
    private TableData<T> data;

    // 获取数据载体
    public TableData<T> getData() {
        if (data == null) {
            data = new TableData<>(this.total, this.rows);
        }
        return data;
    }

    // 设置列表数据并同步更新内部载体
    public void setRows(List<T> rows) {
        this.rows = rows;
        if (this.data != null) {
            this.data.setRows(rows);
        }
    }

    // 设置总记录数并同步更新内部载体
    public void setTotal(long total) {
        this.total = total;
        if (this.data != null) {
            this.data.setTotal(total);
        }
    }

    // 未查出任何数据时调用
    public static <T> TableDataResult<T> empty() {
        TableDataResult<T> rspData = new TableDataResult<>();
        rspData.setCode(ResultCode.SUCCESS.getCode());
        rspData.setRows(new ArrayList<>());
        rspData.setMsg(ResultCode.SUCCESS.getMsg());
        rspData.setTotal(0);
        rspData.setData(new TableData<>(0, rspData.getRows()));
        return rspData;
    }

    // 查出数据时调用
    public static <T> TableDataResult<T> success(List<T> list, long total) {
        TableDataResult<T> rspData = new TableDataResult<>();
        rspData.setCode(ResultCode.SUCCESS.getCode());
        rspData.setRows(list != null ? list : Collections.emptyList());
        rspData.setMsg(ResultCode.SUCCESS.getMsg());
        rspData.setTotal(total);
        rspData.setData(new TableData<>(total, rspData.getRows()));
        return rspData;
    }

    // 发生业务失败时调用
    public static <T> TableDataResult<T> fail(ResultCode resultCode) {
        TableDataResult<T> rspData = new TableDataResult<>();
        rspData.setCode(resultCode.getCode());
        rspData.setMsg(resultCode.getMsg());
        rspData.setTotal(0);
        rspData.setRows(Collections.emptyList());
        rspData.setData(new TableData<>(0, rspData.getRows()));
        return rspData;
    }

    // 嵌套分页数据载体类
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableData<E> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        // 总记录数
        private long total;

        // 列表数据
        private List<E> rows;
    }
}
