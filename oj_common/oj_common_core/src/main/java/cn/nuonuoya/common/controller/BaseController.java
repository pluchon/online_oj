package cn.nuonuoya.common.controller;

import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import com.github.pagehelper.PageInfo;

import java.util.List;

// 基础控制器，提供统一结果包装与分页组装公共能力
public class BaseController {

    // 判断数据库操作是否成功（按受影响行数）
    public OJResult<Void> toResult(int rows) {
        return rows > 0 ? OJResult.ok() : OJResult.fail();
    }

    // 判断数据库操作是否成功（按布尔结果）
    public OJResult<Void> toResult(boolean result) {
        return result ? OJResult.ok() : OJResult.fail();
    }

    // 响应请求分页数据（统一封装为 TableDataResult）
    protected <T> TableDataResult<T> getTableData(List<T> list) {
        if (list == null) {
            return TableDataResult.empty();
        }
        return TableDataResult.success(list, new PageInfo<>(list).getTotal());
    }

    // 响应请求分页数据（兼容 getTableDataInfo 命名）
    protected <T> TableDataResult<T> getTableDataInfo(List<T> list) {
        return getTableData(list);
    }
}
