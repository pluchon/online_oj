package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.system.dto.UserDTO;
import cn.nuonuoya.system.dto.UserEditDTO;
import cn.nuonuoya.system.dto.UserStatusDTO;
import cn.nuonuoya.system.service.UserService;
import cn.nuonuoya.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 用户控制器
@Validated
@RestController
@RequestMapping("/user")
@Tag(name = "用户API")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    /** 分页查询用户列表 */
    @GetMapping
    @Operation(summary = "用户列表", description = "支持按用户ID与昵称模糊筛选的分页查询")
    public TableDataResult<UserVO> list(@Validated UserDTO queryDTO) {
        List<UserVO> list = userService.list(queryDTO);
        return getTableData(list);
    }

    /** 编辑用户资料 */
    @PutMapping("/{userId}")
    @Operation(summary = "编辑用户资料", description = "修改用户昵称、性别、手机号、邮箱、学校等资料，手机号需唯一")
    public OJResult<Void> edit(@PathVariable("userId") Long userId, @Validated @RequestBody UserEditDTO editDTO) {
        editDTO.setUserId(userId);
        return toResult(userService.edit(editDTO));
    }

    /** 修改用户状态（拉黑 / 解禁） */
    @PutMapping("/{userId}/status")
    @Operation(summary = "修改用户状态", description = "修改用户状态（0: 拉黑，1: 正常）")
    public OJResult<Void> updateStatus(@PathVariable("userId") Long userId, @Validated @RequestBody UserStatusDTO statusDTO) {
        statusDTO.setUserId(userId);
        return toResult(userService.updateStatus(statusDTO));
    }
}
