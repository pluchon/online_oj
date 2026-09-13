package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.dto.QuestionDTO;
import cn.nuonuoya.system.vo.QuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 题目数据访问接口
@Mapper
public interface QuestionMapper extends BaseMapper<TbQuestion> {

    // 联查题目列表（供PageHelper拦截实现物理分页）
    List<QuestionVO> selectQuestionList(@Param("query") QuestionDTO query);
}
