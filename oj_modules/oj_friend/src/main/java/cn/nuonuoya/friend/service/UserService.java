package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.dto.UserLoginDTO;
import cn.nuonuoya.friend.dto.UserProfileUpdateDTO;
import cn.nuonuoya.friend.dto.UserSendCodeDTO;
import cn.nuonuoya.friend.dto.UserCalendarQueryDTO;
import cn.nuonuoya.friend.dto.UserOverviewQueryDTO;
import cn.nuonuoya.friend.vo.UserCalendarVO;
import cn.nuonuoya.friend.vo.UserOverviewVO;
import cn.nuonuoya.friend.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

// C端用户业务接口
public interface UserService {

    // 发送短信验证码
    void sendCode(UserSendCodeDTO sendCodeDTO);

    // 用户短信验证码登录与注册
    String login(UserLoginDTO loginDTO);

    // 获取当前登录用户个人资料
    UserVO getUserProfile();

    // 更新当前登录用户个人资料
    void updateUserProfile(UserProfileUpdateDTO updateDTO);

    // 上传当前登录用户头像至OSS并更新资料
    String uploadAvatar(MultipartFile file);

    // 获取当前登录用户数据总览统计（支持时间范围筛选）
    UserOverviewVO getUserOverview(UserOverviewQueryDTO queryDTO);

    // 获取当前登录用户解题日历按年份统计
    UserCalendarVO getUserCalendar(UserCalendarQueryDTO queryDTO);

    // 清除指定用户的详情缓存（供管理端修改用户状态后调用）
    void evictUserCache(Long userId);
}


