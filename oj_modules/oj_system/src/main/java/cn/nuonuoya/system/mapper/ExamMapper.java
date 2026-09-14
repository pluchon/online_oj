package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.dto.ExamDTO;
import cn.nuonuoya.system.vo.ExamVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 竞赛数据访问接口
@Mapper
public interface ExamMapper extends BaseMapper<TbExam> {

    // 联查竞赛列表（供PageHelper拦截实现物理分页）
    List<ExamVO> selectExamList(@Param("query") ExamDTO query);
}
