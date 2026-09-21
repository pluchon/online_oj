package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.domain.TbQuestionCase;

import java.util.List;

// 题目测试用例业务接口
public interface QuestionCaseService {

    // 查询题目的公开示例（按排序升序）
    List<TbQuestionCase> listSamples(Long questionId);

    // 查询题目的全部用例（公开示例在前，隐藏用例在后）
    List<TbQuestionCase> listAll(Long questionId);

    // 根据用例ID查询（供提交记录回显首个未通过用例）
    TbQuestionCase getById(Long caseId);
}
