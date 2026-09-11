package cn.nuonuoya.system.converter;

import cn.nuonuoya.system.domain.SysUser;
import cn.nuonuoya.system.vo.SysUserVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 系统用户对象转换器
public class SysUserConverter {

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

    // 实体列表转换为视图对象列表
    public static List<SysUserVO> toVOList(List<SysUser> entityList) {
        if (entityList == null) {
            return Collections.emptyList();
        }
        List<SysUserVO> voList = new ArrayList<>();
        for (SysUser entity : entityList) {
            SysUserVO vo = toVO(entity);
            if (vo != null) {
                voList.add(vo);
            }
        }
        return voList;
    }
}
