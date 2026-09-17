package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目Mapper持久层接口
@Mapper
public interface QuestionMapper extends BaseMapper<TbQuestion> {
}
