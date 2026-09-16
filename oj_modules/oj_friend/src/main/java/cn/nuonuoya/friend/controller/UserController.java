package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.friend.dto.UserLoginDTO;
import cn.nuonuoya.friend.dto.UserSendCodeDTO;
import cn.nuonuoya.friend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
