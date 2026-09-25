package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbQuestionEditorial;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目官方题解数据访问接口
@Mapper
public interface QuestionEditorialMapper extends BaseMapper<TbQuestionEditorial> {
}
