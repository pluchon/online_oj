package cn.nuonuoya.system.service;

import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.system.dto.AppealHandleDTO;
import cn.nuonuoya.system.dto.AppealQueryDTO;
import cn.nuonuoya.system.vo.AppealDetailVO;
import cn.nuonuoya.system.vo.AppealVO;

import java.util.List;
import java.util.Map;

// 申诉管理业务接口（申诉归 oj-friend，本服务补用户、题目与用例信息并发起裁定）
public interface AppealService {

    // 分页查询申诉（按用户ID、题目名称、申诉时间筛选，按申诉时间倒序）
    TableDataResult<AppealVO> list(AppealQueryDTO queryDTO);

    // 查询申诉详情（含逐用例输入、预期与实际输出）
    AppealDetailVO getDetail(Long appealId);

    // 裁定申诉（存疑、通过、不通过）
    void handle(Long appealId, AppealHandleDTO handleDTO);

    // 各题「申诉成立、待修题」的数量：裁定为通过的申诉晚于题目用例最后一次修改才计入
    Map<Long, Integer> countUpheldToFix(List<Long> questionIds);
}
