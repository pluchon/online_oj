package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbExamQuestion;
import cn.nuonuoya.system.vo.QuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 竞赛题目关系数据访问接口
@Mapper
public interface ExamQuestionMapper extends BaseMapper<TbExamQuestion> {

    // 查询指定竞赛关联的题目列表
    List<QuestionVO> selectQuestionListByExamId(@Param("examId") Long examId);
}
