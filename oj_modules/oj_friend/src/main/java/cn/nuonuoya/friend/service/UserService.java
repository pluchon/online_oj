package cn.nuonuoya.friend.service;

import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.friend.dto.UserLoginDTO;
import cn.nuonuoya.friend.dto.UserProfileUpdateDTO;
import cn.nuonuoya.friend.dto.UserSendCodeDTO;
import cn.nuonuoya.friend.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

// C端用户业务接口
public interface UserService {

    // 发送短信验证码
    OJResult<Void> sendCode(UserSendCodeDTO sendCodeDTO);

    // 用户短信验证码登录与注册
    OJResult<String> login(UserLoginDTO loginDTO);

    // 获取当前登录用户个人资料
    OJResult<UserVO> getUserProfile();

    // 更新当前登录用户个人资料
    OJResult<Void> updateUserProfile(UserProfileUpdateDTO updateDTO);

    // 上传当前登录用户头像至OSS并更新资料
    OJResult<String> uploadAvatar(MultipartFile file);
}
