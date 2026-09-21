package cn.nuonuoya.friend.converter;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.judge.dto.JudgeCaseDTO;
import cn.nuonuoya.api.judge.vo.JudgeCaseResultVO;
import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.vo.CaseResultVO;
import cn.nuonuoya.friend.vo.QuestionCaseVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 题目测试用例对象模型转换器
public class QuestionCaseConverter {

    // 将用例实体列表转换为公开示例视图列表（仅展示格式）
    public static List<QuestionCaseVO> toSampleVOList(List<TbQuestionCase> caseList) {
        if (CollUtil.isEmpty(caseList)) {
            return Collections.emptyList();
        }
        List<QuestionCaseVO> voList = new ArrayList<>(caseList.size());
        for (TbQuestionCase questionCase : caseList) {
            QuestionCaseVO vo = new QuestionCaseVO();
            vo.setInput(questionCase.getDisplayInput());
            vo.setOutput(questionCase.getDisplayOutput());
            voList.add(vo);
        }
        return voList;
    }

    // 将用例实体列表转换为判题用例列表（仅判题格式）
    public static List<JudgeCaseDTO> toJudgeCaseList(List<TbQuestionCase> caseList) {
        if (CollUtil.isEmpty(caseList)) {
            return Collections.emptyList();
        }
        List<JudgeCaseDTO> dtoList = new ArrayList<>(caseList.size());
        for (TbQuestionCase questionCase : caseList) {
            JudgeCaseDTO dto = new JudgeCaseDTO();
            dto.setCaseId(questionCase.getCaseId());
            dto.setInput(questionCase.getJudgeInput());
            dto.setExpectedOutput(questionCase.getJudgeOutput());
            dtoList.add(dto);
        }
        return dtoList;
    }

    // 将用例实体与判题逐用例结果按顺序合并为结果视图列表
    public static List<CaseResultVO> toCaseResultVOList(List<TbQuestionCase> caseList, List<JudgeCaseResultVO> judgeResults) {
        if (CollUtil.isEmpty(caseList)) {
            return Collections.emptyList();
        }
        List<CaseResultVO> voList = new ArrayList<>(caseList.size());
        for (int i = 0; i < caseList.size(); i++) {
            JudgeCaseResultVO judgeResult = judgeResults != null && i < judgeResults.size() ? judgeResults.get(i) : null;
            voList.add(toCaseResultVO(
                    caseList.get(i),
                    judgeResult == null ? null : judgeResult.getActualOutput(),
                    judgeResult != null && Boolean.TRUE.equals(judgeResult.getPass())));
        }
        return voList;
    }

    // 将单个用例实体与实际输出转换为结果视图
    public static CaseResultVO toCaseResultVO(TbQuestionCase questionCase, String actualOutput, boolean pass) {
        if (questionCase == null) {
            return null;
        }
        CaseResultVO vo = new CaseResultVO();
        vo.setInput(questionCase.getDisplayInput());
        vo.setExpectedOutput(questionCase.getDisplayOutput());
        vo.setActualOutput(actualOutput);
        vo.setPass(pass);
        return vo;
    }
}
