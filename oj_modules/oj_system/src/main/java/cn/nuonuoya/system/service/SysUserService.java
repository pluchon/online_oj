package cn.nuonuoya.system.service;

import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.dto.SysUserSaveDTO;
import cn.nuonuoya.system.vo.SysUserVO;

import java.util.List;

// 管理端用户业务接口
public interface SysUserService {

    // 管理员登录
    OJResult<String> login(String userAccount, String password);

    // 新增管理员
    int add(SysUserSaveDTO saveDTO);

    // 删除用户
    OJResult<Void> delete(Long userId);

    // 用户详情
    OJResult<SysUserVO> detail(Long userId, String sex);

    // 查询所有用户列表
    OJResult<List<SysUserVO>> list();
}
