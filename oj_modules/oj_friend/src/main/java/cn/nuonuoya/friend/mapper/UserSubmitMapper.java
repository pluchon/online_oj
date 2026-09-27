package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendQuestionStatVO;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 用户代码提交记录持久层
@Mapper
public interface UserSubmitMapper extends BaseMapper<TbUserSubmit> {

    // 按天统计起始时间之后的提交数、已出结论数与通过数（只返回有提交的日期，按日期升序）
    List<FriendDailyStatVO> selectDailyStats(@Param("startTime") LocalDateTime startTime,
                                             @Param("judging") Integer judging,
                                             @Param("pass") Integer pass);

    // 统计起始时间之后有提交的去重用户数
    int countDistinctUsers(@Param("startTime") LocalDateTime startTime);

    // 已出结论提交数达到门槛的题中，按通过率升序取前若干道（已删除的题不计）
    List<FriendQuestionStatVO> selectHardQuestions(@Param("minJudged") int minJudged,
                                                   @Param("limit") int limit,
                                                   @Param("judging") Integer judging,
                                                   @Param("pass") Integer pass);
}
