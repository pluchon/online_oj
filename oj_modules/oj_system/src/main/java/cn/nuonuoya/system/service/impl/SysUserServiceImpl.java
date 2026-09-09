package cn.nuonuoya.system.service.impl;

import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.system.converter.SysUserConverter;
import cn.nuonuoya.system.domain.SysUser;
import cn.nuonuoya.system.dto.SysUserSaveDTO;
import cn.nuonuoya.system.mapper.SysUserMapper;
import cn.nuonuoya.system.service.SysUserService;
import cn.nuonuoya.system.utils.BCryptUtils;
import cn.nuonuoya.system.vo.SysUserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 管理端用户业务实现
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    // 账号搜索查询
    @Override
    public OJResult<Void> login(String userAccount, String password) {
        SysUser loginResult = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getPassword)
                .eq(SysUser::getUserAccount, userAccount));
        // 校验用户存在性
        if (loginResult == null) {
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        // 校验密码
        if (BCryptUtils.matchesPassword(password, loginResult.getPassword())) {
            return OJResult.ok();
        }
        // 登录失败
        return OJResult.fail(ResultCode.FAILED_LOGIN);
    }

    // 新增管理员
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OJResult<Void> add(SysUserSaveDTO saveDTO) {
        if (saveDTO == null || saveDTO.getUserAccount() == null) {
            return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验账号是否已存在
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserAccount, saveDTO.getUserAccount()));
        if (count != null && count > 0) {
            return OJResult.fail(ResultCode.FAILED_USER_EXISTS);
        }
        SysUser sysUser = SysUserConverter.toEntity(saveDTO);
        // 对用户密码进行加密存储
        sysUser.setPassword(BCryptUtils.encryptPassword(sysUser.getPassword()));
        sysUser.setCreateTime(LocalDateTime.now());
        sysUser.setCreateBy(1L);
        sysUserMapper.insert(sysUser);
        return OJResult.ok();
    }

    // 删除用户
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OJResult<Void> delete(Long userId) {
        if (userId == null) {
            return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验用户是否存在
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        sysUserMapper.deleteById(userId);
        return OJResult.ok();
    }

    // 用户详情
    @Override
    public OJResult<SysUserVO> detail(Long userId, String sex) {
        if (userId == null) {
            return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 查询用户并转换为VO
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return OJResult.ok(SysUserConverter.toVO(sysUser));
    }
}
