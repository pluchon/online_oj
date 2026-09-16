package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbUser;
import cn.nuonuoya.system.dto.UserDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 用户数据访问接口
@Mapper
public interface UserMapper extends BaseMapper<TbUser> {

    // 动态条件查询用户列表（供PageHelper拦截实现物理分页）
    List<TbUser> selectUserList(@Param("query") UserDTO query);
}
