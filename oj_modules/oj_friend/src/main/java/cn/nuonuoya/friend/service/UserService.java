package cn.nuonuoya.friend.service;

import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.friend.dto.UserLoginDTO;
import cn.nuonuoya.friend.dto.UserSendCodeDTO;

// C端用户业务接口
public interface UserService {

    // 发送短信验证码
    OJResult<Void> sendCode(UserSendCodeDTO sendCodeDTO);

    // 用户短信验证码登录与注册
    OJResult<String> login(UserLoginDTO loginDTO);
}
