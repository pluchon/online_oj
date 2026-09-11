package cn.nuonuoya.system.controller;

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

    @Operation(summary = "⽤⼾详情", description = "根据查询条件查询⽤⼾详情")
    @GetMapping("/detail")
    @Parameters(value = {
            @Parameter(name = "userId", in = ParameterIn.QUERY, description = "⽤⼾ID"),
            @Parameter(name = "sex", in = ParameterIn.QUERY, description = "⽤⼾性别")
    })
    @ApiResponse(responseCode = "1000", description = "成功获取⽤⼾信息")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    @ApiResponse(responseCode = "3102", description = "⽤⼾不存在")
    public OJResult<SysUserVO> detail(Long userId, @RequestParam(required = false) String sex) {
        return sysUserService.detail(userId, sex);
    }
}
