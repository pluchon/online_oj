package cn.nuonuoya.friend.service;

import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.friend.dto.ExamEnrollDTO;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.vo.ExamVO;
import cn.nuonuoya.friend.vo.UserExamVO;

import java.util.List;

// C端竞赛业务接口
public interface ExamService {

    // 分页查询竞赛列表
    List<ExamVO> list(ExamQueryDTO queryDTO);

    // 分页查询未完赛竞赛列表
    List<ExamVO> getUnfinishList(ExamQueryDTO queryDTO);

    // 分页查询历史竞赛列表
    List<ExamVO> getHistoryList(ExamQueryDTO queryDTO);

    // 报名参加竞赛
    void enroll(ExamEnrollDTO enrollDTO);

    // 分页查询当前用户已报名的竞赛列表
    List<UserExamVO> getMyExamList(PageQuery pageQuery);
}
