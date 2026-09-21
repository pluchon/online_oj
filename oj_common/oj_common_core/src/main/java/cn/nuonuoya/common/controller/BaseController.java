package cn.nuonuoya.common.controller;

import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import com.github.pagehelper.PageInfo;

import java.util.List;

// 基础控制器，提供统一结果包装与分页组装公共能力
public class BaseController {

    // 按受影响行数返回操作结果
    public OJResult<Void> toResult(int rows) {
        return rows > 0 ? OJResult.ok() : OJResult.fail();
    }

    // 按布尔结果返回操作结果
    public OJResult<Void> toResult(boolean result) {
        return result ? OJResult.ok() : OJResult.fail();
    }

    // 将 PageHelper 分页查询结果封装为分页响应
    protected <T> TableDataResult<T> getTableData(List<T> list) {
        if (list == null) {
            return TableDataResult.empty();
        }
        return TableDataResult.success(list, new PageInfo<>(list).getTotal());
    }
}
