package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbExam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// C端竞赛数据访问接口
@Mapper
public interface ExamMapper extends BaseMapper<TbExam> {
}
