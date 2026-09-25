package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目标签持久层
@Mapper
public interface TagMapper extends BaseMapper<TbTag> {
}
