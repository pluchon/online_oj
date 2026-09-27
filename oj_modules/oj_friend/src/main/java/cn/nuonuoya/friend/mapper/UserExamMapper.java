package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.api.friend.vo.FriendExamStatVO;
import cn.nuonuoya.friend.domain.TbUserExam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 用户竞赛关联数据访问接口
@Mapper
public interface UserExamMapper extends BaseMapper<TbUserExam> {

    // 指定竞赛各自的报名人数（只返回有报名的竞赛）
    List<FriendExamStatVO> selectEnrollCounts(@Param("examIds") List<Long> examIds);

    // 指定竞赛合计的报名人数（跨竞赛按用户去重）
    int countDistinctEnrolled(@Param("examIds") List<Long> examIds);
}
