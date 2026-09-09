package cn.nuonuoya.system.converter;

import cn.nuonuoya.system.domain.SysUser;
import cn.nuonuoya.system.dto.SysUserSaveDTO;
import cn.nuonuoya.system.vo.SysUserVO;

// 系统用户对象转换器
public class SysUserConverter {

    // 保存参数转换为实体对象
    public static SysUser toEntity(SysUserSaveDTO saveDTO) {
        if (saveDTO == null) {
            return null;
        }
        SysUser sysUser = new SysUser();
        sysUser.setUserAccount(saveDTO.getUserAccount());
        sysUser.setPassword(saveDTO.getPassword());
        sysUser.setNickName(saveDTO.getNickName());
        return sysUser;
    }

    // 实体对象转换为视图对象
    public static SysUserVO toVO(SysUser entity) {
        if (entity == null) {
            return null;
        }
        SysUserVO vo = new SysUserVO();
        vo.setUserId(entity.getUserId());
        vo.setUserAccount(entity.getUserAccount());
        vo.setNickName(entity.getNickName());
        return vo;
    }
}
