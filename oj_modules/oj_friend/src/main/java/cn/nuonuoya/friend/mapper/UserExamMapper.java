package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.api.friend.vo.FriendExamStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.friend.domain.TbUserExam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 用户竞赛关联数据访问接口
@Mapper
public interface UserExamMapper extends BaseMapper<TbUserExam> {

    // 指定竞赛各自的报名人数与参赛人数（交过代码的去重用户；两项都为 0 的竞赛不返回）
    List<FriendExamStatVO> selectExamStats(@Param("examIds") List<Long> examIds);

    // 指定竞赛合计的报名与参赛人数（跨竞赛按用户去重，竞赛列表由调用方另行填充）
    FriendExamSummaryVO selectExamSummary(@Param("examIds") List<Long> examIds);
}
