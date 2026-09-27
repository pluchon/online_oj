package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.friend.enums.AppealStatusEnum;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendAppealVO;
import cn.nuonuoya.api.friend.vo.FriendCaseResultVO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.enums.QuestionCaseType;
import cn.nuonuoya.system.vo.AppealBaseVO;
import cn.nuonuoya.system.vo.AppealCaseVO;
import cn.nuonuoya.system.vo.AppealDetailVO;
import cn.nuonuoya.system.vo.AppealVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// 申诉对象转换器
public class AppealConverter {

    // 逐用例状态串：通过
    private static final char CASE_PASS = '1';

    // 逐用例状态串：未通过
    private static final char CASE_FAIL = '0';

    private AppealConverter() {
    }

    // C端申诉列表转换为管理端视图（昵称与题目名称由调用方补充）
    public static List<AppealVO> toVOList(List<FriendAppealVO> sourceList) {
        List<AppealVO> voList = BeanUtil.copyToList(sourceList, AppealVO.class);
        voList.forEach(AppealConverter::fillDesc);
        return voList;
    }

    // C端申诉详情与题目当前的用例合并为管理端详情
    public static AppealDetailVO toDetailVO(FriendAppealDetailVO source, List<TbQuestionCase> cases) {
        AppealDetailVO vo = BeanUtil.copyProperties(source, AppealDetailVO.class, "cases");
        fillDesc(vo);
        vo.setCases(toCaseList(source, cases));
        return vo;
    }

    // 逐用例结果：有记录时按判题时的用例ID对应（用例已被修改删除的标出），早期提交按当前用例顺序对应逐用例状态串
    private static List<AppealCaseVO> toCaseList(FriendAppealDetailVO source, List<TbQuestionCase> cases) {
        Map<Long, TbQuestionCase> caseById = cases.stream().collect(Collectors.toMap(TbQuestionCase::getCaseId, Function.identity()));
        List<AppealCaseVO> result = new ArrayList<>();
        if (CollUtil.isNotEmpty(source.getCaseResults())) {
            List<FriendCaseResultVO> records = source.getCaseResults();
            for (int i = 0; i < records.size(); i++) {
                FriendCaseResultVO record = records.get(i);
                result.add(toCase(i + 1, record.getCaseId(), record.getPass(), record.getOutput(), caseById.get(record.getCaseId())));
            }
            return result;
        }
        String states = source.getCaseStates() == null ? "" : source.getCaseStates();
        for (int i = 0; i < cases.size(); i++) {
            TbQuestionCase questionCase = cases.get(i);
            Boolean pass = i >= states.length() ? null
                    : states.charAt(i) == CASE_PASS ? Boolean.TRUE : states.charAt(i) == CASE_FAIL ? Boolean.FALSE : null;
            String output = Objects.equals(questionCase.getCaseId(), source.getFailCaseId()) ? source.getFailOutput() : null;
            result.add(toCase(i + 1, questionCase.getCaseId(), pass, output, questionCase));
        }
        return result;
    }

    // 单个用例：通过的用例实际输出同预期输出
    private static AppealCaseVO toCase(int index, Long caseId, Boolean pass, String output, TbQuestionCase questionCase) {
        AppealCaseVO vo = new AppealCaseVO();
        vo.setIndex(index);
        vo.setCaseId(caseId);
        vo.setPass(pass);
        vo.setMissing(questionCase == null);
        if (questionCase != null) {
            vo.setSample(Objects.equals(QuestionCaseType.SAMPLE.getValue(), questionCase.getIsSample()));
            vo.setInput(questionCase.getJudgeInput());
            vo.setExpectedOutput(questionCase.getJudgeOutput());
        }
        vo.setActualOutput(Boolean.TRUE.equals(pass) && questionCase != null ? questionCase.getJudgeOutput() : output);
        return vo;
    }

    // 补申诉状态与原判题结论的描述
    private static void fillDesc(AppealBaseVO vo) {
        AppealStatusEnum status = AppealStatusEnum.getByCode(vo.getStatus());
        vo.setStatusDesc(status == null ? null : status.getDesc());
        JudgeStatusEnum origin = JudgeStatusEnum.getByCode(vo.getOriginJudgeStatus());
        vo.setOriginJudgeStatusDesc(origin == null ? null : origin.getDesc());
    }
}
