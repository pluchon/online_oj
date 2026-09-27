package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbSubmitAppeal;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 提交申诉持久层
@Mapper
public interface SubmitAppealMapper extends BaseMapper<TbSubmitAppeal> {
}
