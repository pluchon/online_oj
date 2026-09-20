package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbExamQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 竞赛题目关系持久层
@Mapper
public interface ExamQuestionMapper extends BaseMapper<TbExamQuestion> {
}
