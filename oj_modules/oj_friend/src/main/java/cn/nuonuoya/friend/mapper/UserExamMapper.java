package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbUserExam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 用户竞赛关联数据访问接口
@Mapper
public interface UserExamMapper extends BaseMapper<TbUserExam> {
}
