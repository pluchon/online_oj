package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbQuestionTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目标签关联持久层
@Mapper
public interface QuestionTagMapper extends BaseMapper<TbQuestionTag> {
}
