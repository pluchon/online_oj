package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.friend.vo.FriendCaseResultVO;
import cn.nuonuoya.api.judge.vo.JudgeCaseResultVO;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.vo.QuestionRunResultVO;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;
import cn.nuonuoya.friend.vo.UserSubmitResultVO;
import com.alibaba.fastjson2.JSON;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 用户提交记录对象模型转换器
public class UserSubmitConverter {

    // 回显文本字段最大长度（与表字段 varchar(2000) 对齐）
    private static final int MAX_TEXT_LENGTH = 2000;

    // 逐用例状态串最大长度（与表字段 varchar(500) 对齐）
    private static final int MAX_CASE_STATES_LENGTH = 500;

    // 单个用例实际输出最多保存的字数
    private static final int MAX_CASE_OUTPUT_LENGTH = 300;

    // 逐用例状态：通过
    private static final char CASE_PASS = '1';

    // 逐用例状态：未通过
    private static final char CASE_FAIL = '0';

    // 逐用例状态：未执行
    private static final char CASE_SKIPPED = '-';

    // 将提交记录实体列表转换为本题提交记录视图列表（判题结论字段名不同，单独赋值）
    public static List<SubmitHistoryVO> toHistoryVOList(List<TbUserSubmit> submitList) {
        if (CollUtil.isEmpty(submitList)) {
            return Collections.emptyList();
        }
        List<SubmitHistoryVO> voList = BeanUtil.copyToList(submitList, SubmitHistoryVO.class);
        for (int i = 0; i < voList.size(); i++) {
            voList.get(i).setStatus(submitList.get(i).getJudgeStatus());
        }
        return voList;
    }

    // 将提交记录转换为评测结果视图（判题结论字段名不同，单独赋值；首个未通过用例由调用方补充）
    public static UserSubmitResultVO toResultVO(TbUserSubmit submit) {
        UserSubmitResultVO vo = BeanUtil.copyProperties(submit, UserSubmitResultVO.class);
        vo.setStatus(submit.getJudgeStatus());
        return vo;
    }

    // 将示例用例运行结果转换为视图（逐用例结果与示例按顺序合并）
    public static QuestionRunResultVO toRunResultVO(JudgeResultVO judgeResult, List<TbQuestionCase> sampleList) {
        QuestionRunResultVO vo = BeanUtil.copyProperties(judgeResult, QuestionRunResultVO.class, "caseResults");
        vo.setCaseResults(QuestionCaseConverter.toCaseResultVOList(sampleList, judgeResult.getCaseResults()));
        return vo;
    }

    // 将判题结果转换为提交记录更新实体（超长文本按表字段长度截断）
    public static TbUserSubmit toJudgedEntity(JudgeResultVO resultVO) {
        TbUserSubmit submit = new TbUserSubmit();
        submit.setSubmitId(resultVO.getSubmitId());
        submit.setPass(resultVO.getPass() != null ? resultVO.getPass() : SubmitPassEnum.NOT_PASS.getCode());
        submit.setScore(resultVO.getScore() != null ? resultVO.getScore() : 0);
        submit.setExeMessage(StrUtil.sub(StrUtil.nullToEmpty(resultVO.getExeMessage()), 0, MAX_TEXT_LENGTH));
        submit.setJudgeStatus(resultVO.getStatus());
        submit.setPassCount(resultVO.getPassCount() != null ? resultVO.getPassCount() : 0);
        submit.setTotalCount(resultVO.getTotalCount() != null ? resultVO.getTotalCount() : 0);
        submit.setTimeCost(resultVO.getTimeCost() != null ? resultVO.getTimeCost().intValue() : null);
        submit.setFailCaseId(resultVO.getFailCaseId());
        submit.setFailOutput(resultVO.getFailOutput() == null ? null : StrUtil.sub(resultVO.getFailOutput(), 0, MAX_TEXT_LENGTH));
        submit.setCaseStates(buildCaseStates(resultVO));
        submit.setCaseOutputs(buildCaseOutputs(resultVO));
        submit.setUpdateTime(LocalDateTime.now());
        return submit;
    }

    // 将逐用例结果编码为状态串（1: 通过 0: 未通过 -: 未执行）
    private static String buildCaseStates(JudgeResultVO resultVO) {
        List<JudgeCaseResultVO> caseResults = resultVO.getCaseResults();
        if (CollUtil.isEmpty(caseResults)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(caseResults.size());
        for (JudgeCaseResultVO caseResult : caseResults) {
            Boolean pass = casePass(caseResult, resultVO.getFailCaseId());
            sb.append(pass == null ? CASE_SKIPPED : (pass ? CASE_PASS : CASE_FAIL));
        }
        return StrUtil.sub(sb.toString(), 0, MAX_CASE_STATES_LENGTH);
    }

    // 将逐用例结果编码为 JSON：[{caseId, pass, output}]，只保存未通过用例的实际输出（截断），供申诉时逐个用例查看
    private static String buildCaseOutputs(JudgeResultVO resultVO) {
        List<JudgeCaseResultVO> caseResults = resultVO.getCaseResults();
        if (CollUtil.isEmpty(caseResults)) {
            return null;
        }
        List<FriendCaseResultVO> items = new ArrayList<>(caseResults.size());
        for (JudgeCaseResultVO caseResult : caseResults) {
            FriendCaseResultVO item = new FriendCaseResultVO();
            item.setCaseId(caseResult.getCaseId());
            item.setPass(casePass(caseResult, resultVO.getFailCaseId()));
            if (Boolean.FALSE.equals(item.getPass()) && caseResult.getActualOutput() != null) {
                item.setOutput(StrUtil.maxLength(caseResult.getActualOutput(), MAX_CASE_OUTPUT_LENGTH));
            }
            items.add(item);
        }
        return JSON.toJSONString(items);
    }

    // 单个用例是否通过：通过为 true，有实际输出或是首个失败用例为 false，其余视为未执行（null）
    private static Boolean casePass(JudgeCaseResultVO caseResult, Long failCaseId) {
        if (Boolean.TRUE.equals(caseResult.getPass())) {
            return true;
        }
        boolean failed = caseResult.getActualOutput() != null
                || (caseResult.getCaseId() != null && caseResult.getCaseId().equals(failCaseId));
        return failed ? false : null;
    }
}
