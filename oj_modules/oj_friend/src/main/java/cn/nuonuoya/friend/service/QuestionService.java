package cn.nuonuoya.friend.service;

import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.dto.QuestionQueryDTO;
import cn.nuonuoya.friend.vo.QuestionVO;

// 题目业务服务接口
public interface QuestionService {

    // 分页全文检索题目列表
    TableDataResult<QuestionVO> search(QuestionQueryDTO queryDTO);

    // 查询题目详情
    QuestionVO getDetail(Long questionId);

    // 全量同步MySQL题目数据至ES索引
    int syncAllQuestionsToEs();
}
