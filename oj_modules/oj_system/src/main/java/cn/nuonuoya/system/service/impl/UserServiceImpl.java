package cn.nuonuoya.system.service.impl;

import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.converter.UserConverter;
import cn.nuonuoya.system.domain.TbUser;
import cn.nuonuoya.system.dto.UserDTO;
import cn.nuonuoya.system.dto.UserStatusDTO;
import cn.nuonuoya.system.enums.UserStatus;
import cn.nuonuoya.system.mapper.UserMapper;
import cn.nuonuoya.system.service.UserService;
import cn.nuonuoya.system.vo.UserVO;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

// 用户业务实现类
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;

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

        // 同步清除C端用户的Redis详情缓存，确保切面校验即时感知
        redisService.deleteObject("user:detail:" + statusDTO.getUserId());

        return rows;
    }
}
