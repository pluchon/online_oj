package cn.nuonuoya.system.service.impl;

import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.client.FriendUserClient;
import cn.nuonuoya.system.converter.UserConverter;
import cn.nuonuoya.system.domain.TbUser;
import cn.nuonuoya.system.dto.UserDTO;
import cn.nuonuoya.system.dto.UserEditDTO;
import cn.nuonuoya.system.dto.UserStatusDTO;
import cn.nuonuoya.system.enums.UserStatus;
import cn.nuonuoya.system.mapper.UserMapper;
import cn.nuonuoya.system.service.UserService;
import cn.nuonuoya.mybatis.utils.TransactionUtils;
import cn.nuonuoya.system.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

// 用户业务实现类
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FriendUserClient friendUserClient;

    // 分页多条件查询用户列表实现
    @Override
    public List<UserVO> list(UserDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new UserDTO();
        }
        if (queryDTO.getNickName() != null) {
            queryDTO.setNickName(queryDTO.getNickName().trim());
        }
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<TbUser> list = userMapper.selectUserList(queryDTO);
        return UserConverter.toVOList(list);
    }

    // 编辑用户资料实现（手机号为C端登录凭据，需全局唯一）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int edit(UserEditDTO editDTO) {
        if (userMapper.selectById(editDTO.getUserId()) == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        TbUser updateEntity = UserConverter.toEditEntity(editDTO);
        Long phoneUsed = userMapper.selectCount(new LambdaQueryWrapper<TbUser>()
                .eq(TbUser::getPhone, updateEntity.getPhone())
                .ne(TbUser::getUserId, editDTO.getUserId()));
        if (phoneUsed != null && phoneUsed > 0) {
            throw new ServiceException(ResultCode.FAILED_PHONE_EXISTS);
        }
        int rows = userMapper.updateById(updateEntity);
        evictUserCacheAfterCommit(editDTO.getUserId());
        return rows;
    }

    // 修改用户状态（拉黑 / 解禁）实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateStatus(UserStatusDTO statusDTO) {
        if (statusDTO == null || statusDTO.getUserId() == null || statusDTO.getStatus() == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 状态值合法性校验：仅支持 0(拉黑) 或 1(正常)
        if (!statusDTO.getStatus().equals(UserStatus.BANNED.getValue())
                && !statusDTO.getStatus().equals(UserStatus.NORMAL.getValue())) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验目标用户是否存在
        TbUser user = userMapper.selectById(statusDTO.getUserId());
        if (user == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        // 幂等保护：若当前状态已为目标状态，直接返回成功，避免重复更新
        if (Objects.equals(user.getStatus(), statusDTO.getStatus())) {
            return 1;
        }
        TbUser updateEntity = new TbUser();
        updateEntity.setUserId(statusDTO.getUserId());
        updateEntity.setStatus(statusDTO.getStatus());
        int rows = userMapper.updateById(updateEntity);

        evictUserCacheAfterCommit(statusDTO.getUserId());
        return rows;
    }

    // 事务提交后通知C端清除用户缓存，避免提交前被旧数据重新回填
    private void evictUserCacheAfterCommit(Long userId) {
        TransactionUtils.afterCommit(() -> {
            if (!friendUserClient.evictUserCache(userId)) {
                log.warn("用户数据已更新但C端缓存未清除，将在缓存过期后生效, userId = {}", userId);
            }
        });
    }
}
