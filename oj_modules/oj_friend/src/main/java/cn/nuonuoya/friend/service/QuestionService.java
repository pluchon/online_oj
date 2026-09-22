package cn.nuonuoya.friend.service;

import cn.nuonuoya.api.friend.dto.FriendQuestionCandidateQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendQuestionCandidateVO;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.dto.QuestionQueryDTO;
import cn.nuonuoya.friend.vo.QuestionPreNextVO;
import cn.nuonuoya.friend.vo.QuestionStatsVO;
import cn.nuonuoya.friend.vo.QuestionVO;

import java.util.List;

// 题目业务服务接口
public interface QuestionService {

    // 分页全文检索题目列表
    TableDataResult<QuestionVO> search(QuestionQueryDTO queryDTO);

    // AI 帮建竞赛的候选题目（混合检索 + 通过率）
    List<FriendQuestionCandidateVO> listCandidates(FriendQuestionCandidateQueryDTO queryDTO);

    // 相似题推荐（排除当前题与当前用户已通过的题）
    List<QuestionVO> listSimilar(Long questionId);

    // 查询题目详情
    QuestionVO getDetail(Long questionId);

    // 获取题库总题数与当前学员解题统计信息
    QuestionStatsVO getStats();

    // 题目数据变更后刷新：清除题目顺序缓存并全量同步ES，返回同步题数
    int refreshQuestionData();

    // 获取题目上一题与下一题导航信息
    QuestionPreNextVO getPreAndNext(Long questionId, Long examId);

    // 获取首道题目ID
    Long getFirstQuestionId(Long examId);
}
