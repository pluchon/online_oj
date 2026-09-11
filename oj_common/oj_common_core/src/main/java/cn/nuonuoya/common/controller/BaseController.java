package cn.nuonuoya.common.controller;

import cn.nuonuoya.common.domain.OJResult;

// 公共操作
public class BaseController {

    // 判断数据库操作是否成功
    public OJResult<Void> toResult(int rows){
        return rows > 0 ? OJResult.ok() : OJResult.fail();
    }

    public OJResult<Void> toResult(boolean result){
        return result ? OJResult.ok() : OJResult.fail();
    }
}
