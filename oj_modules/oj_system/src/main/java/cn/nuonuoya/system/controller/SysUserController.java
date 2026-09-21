package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.dto.SysUserSaveDTO;
import cn.nuonuoya.system.dto.UserLoginDTO;
import cn.nuonuoya.system.service.SysUserService;
import cn.nuonuoya.system.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 管理员用户控制器
@RestController
@RequestMapping("/sysUser")
@Tag(name = "管理员用户API")
public class SysUserController extends BaseController {

    @Autowired
    private SysUserService sysUserService;

    /** 管理员账号密码登录 */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "校验账号密码并返回令牌")
    public OJResult<String> login(@Validated @RequestBody UserLoginDTO loginDTO) {
        return OJResult.ok(sysUserService.login(loginDTO.getUserAccount(), loginDTO.getPassword()));
    }

    /** 新增管理员 */
    @PostMapping
    @Operation(summary = "新增管理员", description = "账号唯一，密码加密存储")
    public OJResult<Void> add(@Validated @RequestBody SysUserSaveDTO saveDTO) {
        return toResult(sysUserService.add(saveDTO));
    }

    /** 删除管理员 */
    @DeleteMapping("/{userId}")
    @Operation(summary = "删除管理员", description = "按ID删除管理员，不能删除当前登录账号")
    public OJResult<Void> delete(@PathVariable("userId") Long userId) {
        sysUserService.delete(userId);
        return OJResult.ok();
    }

    /** 获取当前登录管理员信息 */
    @GetMapping("/me")
    @Operation(summary = "当前管理员信息", description = "获取当前登录管理员的昵称")
    public OJResult<SysUserVO> detail() {
        return OJResult.ok(sysUserService.getCurrentUser());
    }

    /** 当前管理员退出登录 */
    @DeleteMapping("/logout")
    @Operation(summary = "退出登录", description = "销毁当前管理员会话")
    public OJResult<Void> logout() {
        sysUserService.logout();
        return OJResult.ok();
    }
}
