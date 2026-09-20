package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbMessageText;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 消息正文持久层接口
@Mapper
public interface MessageTextMapper extends BaseMapper<TbMessageText> {
}
