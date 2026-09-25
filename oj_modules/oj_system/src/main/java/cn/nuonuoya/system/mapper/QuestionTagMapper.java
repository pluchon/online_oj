package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbQuestionTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目标签关联数据访问接口
@Mapper
public interface QuestionTagMapper extends BaseMapper<TbQuestionTag> {
}
