package cn.nuonuoya.system.service.impl;

import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.enums.UserIdentity;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.service.TokenService;
import cn.nuonuoya.security.utils.SecurityUtils;
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

import java.util.Objects;

// 管理端用户业务实现
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private TokenService tokenService;

    // 管理员登录：校验账号密码后签发令牌
    @Override
    public String login(String userAccount, String password) {
        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getUserId, SysUser::getPassword, SysUser::getNickName)
                .eq(SysUser::getUserAccount, userAccount.trim()));
        if (sysUser == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        if (!BCryptUtils.matchesPassword(password, sysUser.getPassword())) {
            throw new ServiceException(ResultCode.FAILED_LOGIN);
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setIdentity(UserIdentity.ADMIN.getValue());
        loginUser.setNickName(sysUser.getNickName());
        return tokenService.createToken(sysUser.getUserId(), loginUser);
    }

    // 新增管理员（账号去除首尾空白后校验唯一，密码加密存储）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(SysUserSaveDTO saveDTO) {
        String userAccount = saveDTO.getUserAccount().trim();
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserAccount, userAccount));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_USER_EXISTS);
        }
        SysUser sysUser = SysUserConverter.toEntity(saveDTO, userAccount, BCryptUtils.encryptPassword(saveDTO.getPassword()));
        return sysUserMapper.insert(sysUser);
    }

    // 删除管理员（不允许删除当前登录账号）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        if (Objects.equals(userId, SecurityUtils.getUserId())) {
            throw new ServiceException(ResultCode.FAILED_SYS_USER_DELETE_SELF);
        }
        if (sysUserMapper.selectById(userId) == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        sysUserMapper.deleteById(userId);
    }

    // 获取当前登录管理员信息（从会话缓存读取）
    @Override
    public SysUserVO getCurrentUser() {
        LoginUser loginUser = tokenService.getLoginUserByKey(SecurityUtils.getUserKey());
        if (loginUser == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        return SysUserConverter.toVO(loginUser);
    }

    // 当前管理员退出登录
    @Override
    public void logout() {
        tokenService.deleteLoginUserByKey(SecurityUtils.getUserKey());
    }
}
