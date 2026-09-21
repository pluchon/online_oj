package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.SysUserSaveDTO;
import cn.nuonuoya.system.vo.SysUserVO;

// 管理端用户业务接口
public interface SysUserService {

    // 管理员登录，返回令牌
    String login(String userAccount, String password);

    // 新增管理员
    int add(SysUserSaveDTO saveDTO);

    // 删除管理员
    void delete(Long userId);

    // 获取当前登录管理员信息
    SysUserVO getCurrentUser();

    // 当前管理员退出登录
    void logout();
}
