package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.UserDTO;
import cn.nuonuoya.system.dto.UserStatusDTO;
import cn.nuonuoya.system.vo.UserVO;

import java.util.List;

// 用户业务接口
public interface UserService {

    // 分页多条件查询用户列表
    List<UserVO> list(UserDTO queryDTO);

    // 修改用户状态（拉黑 / 解禁）
    int updateStatus(UserStatusDTO statusDTO);
}
