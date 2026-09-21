package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbQuestionCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 题目测试用例持久层
@Mapper
public interface QuestionCaseMapper extends BaseMapper<TbQuestionCase> {
}
