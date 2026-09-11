package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.service.SysUserService;
import cn.nuonuoya.system.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 测试控制器
@RestController
@RequestMapping("/test")
@Tag(name = "测试验证API")
public class TestController {

    // 系统用户业务服务
    @Autowired
    private SysUserService sysUserService;

    /** 查询所有管理员用户列表，用于验证网关JWT鉴权 */
    @GetMapping("/list")
    @Operation(summary = "查询用户列表", description = "用于验证网关鉴权通过后能够正常获取数据库数据")
    public OJResult<List<SysUserVO>> list() {
        return sysUserService.list();
    }
}
