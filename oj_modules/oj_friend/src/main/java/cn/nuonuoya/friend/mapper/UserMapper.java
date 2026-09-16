package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// C端用户数据访问接口
@Mapper
public interface UserMapper extends BaseMapper<TbUser> {
}
