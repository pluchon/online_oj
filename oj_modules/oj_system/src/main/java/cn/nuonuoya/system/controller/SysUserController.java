package cn.nuonuoya.system.controller;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.dto.SysUserSaveDTO;
import cn.nuonuoya.system.dto.UserLoginDTO;
import cn.nuonuoya.system.service.SysUserService;
import cn.nuonuoya.system.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 管理员用户控制器
@RestController
@RequestMapping("/sysUser")
@Tag(name = "管理员⽤⼾API")
public class SysUserController extends BaseController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "需要管理员的用户名以及密码，通过JSON传入")
    @ApiResponse(responseCode = "1000", description = "操作成功")
    @ApiResponse(responseCode = "3102", description = "⽤⼾不存在")
    @ApiResponse(responseCode = "3103", description = "⽤⼾名或密码错误")
    public OJResult<String> login(@RequestBody UserLoginDTO userLoginDTO) {
        return sysUserService.login(userLoginDTO.getUserAccount(), userLoginDTO.getPassword());
    }

    /** 新增管理员用户 */
    @Operation(summary = "新增管理员", description = "根据提供的信息新增管理员⽤⼾")
    @PostMapping("/add")
    @ApiResponse(responseCode = "1000", description = "操作成功")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    public OJResult<Void> add(@RequestBody SysUserSaveDTO saveDTO) {
        return toResult(sysUserService.add(saveDTO));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除⽤⼾", description = "通过⽤⼾id删除⽤⼾")
    @Parameters(value = {
            @Parameter(name = "userId", in = ParameterIn.PATH, description = "⽤⼾ID")
    })
    @ApiResponse(responseCode = "1000", description = "成功删除⽤⼾")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    @ApiResponse(responseCode = "3102", description = "⽤⼾不存在")
    public OJResult<Void> delete(@PathVariable Long userId) {
        return sysUserService.delete(userId);
    }

    /** 获取管理员详情 */
    @Operation(summary = "用户详情", description = "获取当前登录管理员详情")
    @GetMapping("/detail")
    @ApiResponse(responseCode = "1000", description = "成功获取用户信息")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    @ApiResponse(responseCode = "3001", description = "未授权或登录已过期")
    @ApiResponse(responseCode = "3102", description = "用户不存在")
    public OJResult<SysUserVO> detail(@RequestHeader(name = HttpConstants.AUTHENTICATION, required = false) String token,
                                      @RequestHeader(name = "token", required = false) String tokenBackup) {
        String finalToken = StrUtil.isNotEmpty(token) ? token : tokenBackup;
        return sysUserService.detail(finalToken);
    }

    /** 管理员退出登录 */
    @Operation(summary = "管理员退出登录", description = "销毁当前管理员会话并清除Redis令牌")
    @DeleteMapping("/logout")
    @ApiResponse(responseCode = "1000", description = "成功退出登录")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    public OJResult<Void> logout(@RequestHeader(name = HttpConstants.AUTHENTICATION, required = false) String token,
                                 @RequestHeader(name = "token", required = false) String tokenBackup) {
        String finalToken = StrUtil.isNotEmpty(token) ? token : tokenBackup;
        return toResult(sysUserService.logout(finalToken));
    }
}
