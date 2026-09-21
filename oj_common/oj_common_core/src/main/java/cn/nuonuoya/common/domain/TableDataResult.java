package cn.nuonuoya.common.domain;

import cn.nuonuoya.common.enums.ResultCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

// 分页列表统一响应
@Getter
@Setter
@NoArgsConstructor
public class TableDataResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 总记录数
    private long total;

    // 当前页数据
    private List<T> rows;

    // 状态码
    private int code;

    // 提示信息
    private String msg;

    // 未查出任何数据
    public static <T> TableDataResult<T> empty() {
        return success(Collections.emptyList(), 0);
    }

    // 查出数据
    public static <T> TableDataResult<T> success(List<T> list, long total) {
        TableDataResult<T> result = new TableDataResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setRows(list != null ? list : Collections.emptyList());
        result.setTotal(total);
        return result;
    }
}
