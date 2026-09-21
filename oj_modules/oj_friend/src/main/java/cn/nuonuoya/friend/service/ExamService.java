package cn.nuonuoya.friend.service;

import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.dto.ExamEnrollDTO;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.vo.ExamRankVO;
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
    List<UserExamVO> getMyExamList(ExamQueryDTO queryDTO);

    // 获取指定竞赛详情
    ExamVO getExamDetail(Long examId);

    // 分页查询竞赛选手得分与排名榜单
    TableDataResult<ExamRankVO> getExamRankList(Long examId, PageQuery pageQuery);

    // 结算所有已结束且未结算的竞赛，返回本次结算场数
    int settleFinishedExams();

    // 刷新竞赛列表缓存；examId 非空时同时清除该竞赛的详情与题目顺序缓存
    int refreshExamCache(Long examId);
}
