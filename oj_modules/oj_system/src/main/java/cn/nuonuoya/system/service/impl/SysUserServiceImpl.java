package cn.nuonuoya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.enums.UserIdentity;
import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.service.TokenService;
import cn.nuonuoya.system.converter.SysUserConverter;
import cn.nuonuoya.system.domain.SysUser;
import cn.nuonuoya.system.dto.SysUserSaveDTO;
import cn.nuonuoya.system.mapper.SysUserMapper;
import cn.nuonuoya.system.service.SysUserService;
import cn.nuonuoya.system.utils.BCryptUtils;
import cn.nuonuoya.system.vo.SysUserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 管理端用户业务实现，实时根据配置中心进行刷新配置
@Service
@RefreshScope
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private TokenService tokenService;

    // 账号搜索查询
    @Override
    public OJResult<String> login(String userAccount, String password) {
        // 查出 userId 密码 以及昵称
        SysUser loginResult = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getUserId, SysUser::getPassword, SysUser::getNickName)
                .eq(SysUser::getUserAccount, userAccount));
        // 校验用户存在性
        if (loginResult == null) {
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        // 校验密码
        if (!BCryptUtils.matchesPassword(password, loginResult.getPassword())) {
            return OJResult.fail(ResultCode.FAILED_LOGIN);
        }
        // 构建 LoginUser 写入身份标识和昵称后生成 Token
        LoginUser loginUser = new LoginUser();
        loginUser.setIdentity(UserIdentity.ADMIN.getValue());
        loginUser.setNickName(loginResult.getNickName());
        String token = tokenService.createToken(loginResult.getUserId(), loginUser);
        return OJResult.ok(token);
    }

    // 新增管理员
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(SysUserSaveDTO saveDTO) {
        // 参数合法性判断
        if (saveDTO == null || StrUtil.isEmpty(saveDTO.getUserAccount())) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验账号是否已存在
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserAccount, saveDTO.getUserAccount()));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_USER_EXISTS);
        }
        // 直接创建实体赋值，无需额外转换器
        SysUser sysUser = new SysUser();
        sysUser.setUserAccount(saveDTO.getUserAccount());
        // 对用户密码进行加密存储
        sysUser.setPassword(BCryptUtils.encryptPassword(saveDTO.getPassword()));
        sysUser.setNickName(saveDTO.getNickName());
        return sysUserMapper.insert(sysUser);
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

    // 获取用户详情 直接从Redis读取登录用户信息
    @Override
    public OJResult<SysUserVO> detail(String token) {
        LoginUser loginUser = tokenService.getLoginUser(token);
        if (loginUser == null) {
            return OJResult.fail(ResultCode.FAILED_UNAUTHORIZED);
        }
        SysUserVO vo = new SysUserVO();
        vo.setNickName(loginUser.getNickName());
        return OJResult.ok(vo);
    }

    // 管理员退出登录
    @Override
    public boolean logout(String token) {
        if (StrUtil.isEmpty(token)) {
            return false;
        }
        tokenService.deleteLoginUser(token);
        return true;
    }

    // 查询所有用户列表
    @Override
    public OJResult<List<SysUserVO>> list() {
        List<SysUser> userList = sysUserMapper.selectList(null);
        return OJResult.ok(SysUserConverter.toVOList(userList));
    }
}
