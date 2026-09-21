package cn.nuonuoya.friend.service.impl;

import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.enums.QuestionCaseTypeEnum;
import cn.nuonuoya.friend.mapper.QuestionCaseMapper;
import cn.nuonuoya.friend.service.QuestionCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

// 题目测试用例业务实现类
@Service
public class QuestionCaseServiceImpl implements QuestionCaseService {

    @Autowired
    private QuestionCaseMapper questionCaseMapper;

    // 查询题目的公开示例（按排序升序）
    @Override
    public List<TbQuestionCase> listSamples(Long questionId) {
        if (questionId == null) {
            return Collections.emptyList();
        }
        return questionCaseMapper.selectList(new LambdaQueryWrapper<TbQuestionCase>()
                .eq(TbQuestionCase::getQuestionId, questionId)
                .eq(TbQuestionCase::getIsSample, QuestionCaseTypeEnum.SAMPLE.getCode())
                .orderByAsc(TbQuestionCase::getSortOrder)
                .orderByAsc(TbQuestionCase::getCaseId));
    }

    // 查询题目的全部用例（公开示例在前，隐藏用例在后）
    @Override
    public List<TbQuestionCase> listAll(Long questionId) {
        if (questionId == null) {
            return Collections.emptyList();
        }
        return questionCaseMapper.selectList(new LambdaQueryWrapper<TbQuestionCase>()
                .eq(TbQuestionCase::getQuestionId, questionId)
                .orderByDesc(TbQuestionCase::getIsSample)
                .orderByAsc(TbQuestionCase::getSortOrder)
                .orderByAsc(TbQuestionCase::getCaseId));
    }

    // 根据用例ID查询（供提交记录回显首个未通过用例）
    @Override
    public TbQuestionCase getById(Long caseId) {
        if (caseId == null) {
            return null;
        }
        return questionCaseMapper.selectById(caseId);
    }
}
