package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendFailedSampleVO;
import cn.nuonuoya.api.friend.vo.FriendHardQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendTagStatVO;
import cn.nuonuoya.api.friend.vo.FriendVerdictStatVO;
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

    // 难题分析：达到门槛的每道题的计数、失败最集中的用例与成立的申诉数（按通过率升序）
    List<FriendHardQuestionStatVO> selectHardQuestionStats(@Param("minJudged") int minJudged,
                                                           @Param("judging") Integer judging,
                                                           @Param("pass") Integer pass,
                                                           @Param("notPass") Integer notPass,
                                                           @Param("upheld") Integer upheld);

    // 难题分析：达到门槛的题按标签汇总（按通过率升序；已删除的标签与关联不计）
    List<FriendTagStatVO> selectHardTagStats(@Param("minJudged") int minJudged,
                                             @Param("judging") Integer judging,
                                             @Param("pass") Integer pass);

    // 难题分析：达到门槛的题的未通过提交按判题结论分布（按数量降序）
    List<FriendVerdictStatVO> selectHardVerdictStats(@Param("minJudged") int minJudged,
                                                     @Param("judging") Integer judging,
                                                     @Param("pass") Integer pass,
                                                     @Param("notPass") Integer notPass);

    // 某题最近的未通过提交样本（每个学员取最近一份，caseId 为空时不按用例筛选）
    List<FriendFailedSampleVO> selectFailedSamples(@Param("questionId") Long questionId,
                                                   @Param("caseId") Long caseId,
                                                   @Param("limit") int limit,
                                                   @Param("notPass") Integer notPass);
}
