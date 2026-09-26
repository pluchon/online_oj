package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.QuestionAddDTO;
import cn.nuonuoya.system.dto.QuestionDTO;
import cn.nuonuoya.system.dto.QuestionEditDTO;
import cn.nuonuoya.system.vo.QuestionDetailVO;
import cn.nuonuoya.system.vo.QuestionVO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

// 题目业务接口
public interface QuestionService {

    // 分页查询题目列表
    List<QuestionVO> list(QuestionDTO queryDTO);

    // 新增题目
    int add(QuestionAddDTO addDTO);

    // 获取题目详情
    QuestionDetailVO getDetail(Long questionId);

    // 修改题目
    int edit(QuestionEditDTO editDTO);

    // 删除题目
    int delete(Long questionId);

    // 已公开的题目：至少有一场用到它的竞赛已经结束（这样的题不能再用于新竞赛）
    Set<Long> listPublishedQuestionIds(Collection<Long> questionIds);

    // 公开已结束竞赛的题目：所在竞赛全部结束的竞赛题改为刷题，返回本次公开的题数（定时任务调用，幂等）
    int publishFinishedContestQuestions();
}
