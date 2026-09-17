package cn.nuonuoya.job.mapper;

import cn.nuonuoya.job.domain.TbExam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// 竞赛数据访问接口
@Mapper
public interface ExamMapper extends BaseMapper<TbExam> {
}
