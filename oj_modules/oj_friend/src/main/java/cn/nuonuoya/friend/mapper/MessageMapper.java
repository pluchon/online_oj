package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.dto.MessageQueryDTO;
import cn.nuonuoya.friend.vo.MessageVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 用户消息投递持久层接口
@Mapper
public interface MessageMapper extends BaseMapper<TbMessage> {

    // 联表查询用户消息列表（按类型、关键词筛选，最新在前）
    List<MessageVO> selectUserMessageList(@Param("userId") Long userId, @Param("query") MessageQueryDTO query);
}
