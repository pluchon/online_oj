package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbQuestionCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目测试用例数据访问接口
@Mapper
public interface QuestionCaseMapper extends BaseMapper<TbQuestionCase> {
}
