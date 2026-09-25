package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbQuestionEditorial;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目官方题解持久层
@Mapper
public interface QuestionEditorialMapper extends BaseMapper<TbQuestionEditorial> {
}
