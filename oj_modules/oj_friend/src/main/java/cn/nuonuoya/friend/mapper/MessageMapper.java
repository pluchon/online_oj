package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 用户消息投递持久层接口
@Mapper
public interface MessageMapper extends BaseMapper<TbMessage> {
}
