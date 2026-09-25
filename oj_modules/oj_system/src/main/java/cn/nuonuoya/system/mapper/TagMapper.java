package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目标签数据访问接口
@Mapper
public interface TagMapper extends BaseMapper<TbTag> {
}
