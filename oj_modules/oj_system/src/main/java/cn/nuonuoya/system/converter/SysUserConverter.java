package cn.nuonuoya.system.converter;

import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.system.domain.SysUser;
import cn.nuonuoya.system.dto.SysUserSaveDTO;
import cn.nuonuoya.system.vo.SysUserVO;

// 管理端用户对象转换器
public class SysUserConverter {

    // 新增请求转换为实体（账号与密码由调用方规范化、加密后传入）
    public static SysUser toEntity(SysUserSaveDTO saveDTO, String userAccount, String encodedPassword) {
        SysUser sysUser = new SysUser();
        sysUser.setUserAccount(userAccount);
        sysUser.setPassword(encodedPassword);
        sysUser.setNickName(saveDTO.getNickName());
        return sysUser;
    }

    // 登录会话信息转换为视图对象
    public static SysUserVO toVO(LoginUser loginUser) {
        SysUserVO vo = new SysUserVO();
        vo.setNickName(loginUser.getNickName());
        return vo;
    }
}
