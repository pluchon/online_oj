package cn.nuonuoya.friend.mapper;

import cn.nuonuoya.friend.domain.TbExamReview;
import cn.nuonuoya.friend.vo.ExamReviewQuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 赛后复盘持久层
@Mapper
public interface ExamReviewMapper extends BaseMapper<TbExamReview> {

    // 学员在这场竞赛里每道题的统计（按竞赛题目顺序；没有提交的题计数为 0）与全场提交、通过人数
    List<ExamReviewQuestionVO> selectQuestionStats(@Param("examId") Long examId,
                                                   @Param("userId") Long userId,
                                                   @Param("pass") Integer pass);
}
