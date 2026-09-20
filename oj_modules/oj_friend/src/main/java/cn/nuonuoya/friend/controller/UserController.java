package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.friend.aspect.CheckUserStatus;
import cn.nuonuoya.friend.dto.UserLoginDTO;
import cn.nuonuoya.friend.dto.UserProfileUpdateDTO;
import cn.nuonuoya.friend.dto.UserSendCodeDTO;
import cn.nuonuoya.friend.service.UserService;
import cn.nuonuoya.friend.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// C端用户控制器
@Validated
@RestController
@RequestMapping("/user")
@Tag(name = "C端用户API")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    /** 发送短信验证码 */
    @PostMapping("/send-code")
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送短信验证码")
    public OJResult<Void> sendCode(@Validated @RequestBody UserSendCodeDTO sendCodeDTO) {
        return userService.sendCode(sendCodeDTO);
    }

    /** 用户短信验证码登录与注册 */
    @PostMapping("/login")
    @Operation(summary = "用户短信登录/注册", description = "通过手机号与短信验证码登录，新用户自动注册")
    public OJResult<String> login(@Validated @RequestBody UserLoginDTO loginDTO) {
        return userService.login(loginDTO);
    }

    /** 获取当前登录用户个人资料 */
    @GetMapping("/profile")
    @Operation(summary = "获取当前登录用户个人资料", description = "基于当前登录态查询用户个人详细资料")
    public OJResult<UserVO> getUserProfile() {
        return userService.getUserProfile();
    }

    /** 更新当前登录用户个人资料 */
    @CheckUserStatus
    @PutMapping("/profile")
    @Operation(summary = "更新当前登录用户个人资料", description = "修改用户昵称、性别、邮箱、微信、学校、专业、个人介绍等")
    public OJResult<Void> updateUserProfile(@Validated @RequestBody UserProfileUpdateDTO updateDTO) {
        return userService.updateUserProfile(updateDTO);
    }

    /** 上传当前登录用户头像 */
    @CheckUserStatus
    @PostMapping("/avatar")
    @Operation(summary = "上传当前登录用户头像", description = "上传头像图片至阿里云OSS并更新用户头像")
    public OJResult<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(file);
    }
}
