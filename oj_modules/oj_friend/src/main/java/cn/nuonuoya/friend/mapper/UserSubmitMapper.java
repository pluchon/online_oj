package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbUserSubmit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 用户代码提交记录持久层
@Mapper
public interface UserSubmitMapper extends BaseMapper<TbUserSubmit> {
}
