package cn.nuonuoya.friend.converter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.judge.vo.JudgeCaseResultVO;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;

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

    // 逐用例状态：通过
    private static final char CASE_PASS = '1';

    // 逐用例状态：未通过
    private static final char CASE_FAIL = '0';

    // 逐用例状态：未执行
    private static final char CASE_SKIPPED = '-';

    // 将提交记录实体列表转换为本题提交记录视图列表
    public static List<SubmitHistoryVO> toHistoryVOList(List<TbUserSubmit> submitList) {
        if (CollUtil.isEmpty(submitList)) {
            return Collections.emptyList();
        }
        List<SubmitHistoryVO> voList = new ArrayList<>(submitList.size());
        for (TbUserSubmit submit : submitList) {
            SubmitHistoryVO vo = new SubmitHistoryVO();
            vo.setSubmitId(submit.getSubmitId());
            vo.setPass(submit.getPass());
            vo.setStatus(submit.getJudgeStatus());
            vo.setPassCount(submit.getPassCount());
            vo.setTotalCount(submit.getTotalCount());
            vo.setTimeCost(submit.getTimeCost());
            vo.setScore(submit.getScore());
            vo.setUserCode(submit.getUserCode());
            vo.setCreateTime(submit.getCreateTime());
            voList.add(vo);
        }
        return voList;
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
            if (Boolean.TRUE.equals(caseResult.getPass())) {
                sb.append(CASE_PASS);
            } else if (caseResult.getActualOutput() != null
                    || (caseResult.getCaseId() != null && caseResult.getCaseId().equals(resultVO.getFailCaseId()))) {
                sb.append(CASE_FAIL);
            } else {
                sb.append(CASE_SKIPPED);
            }
        }
        return StrUtil.sub(sb.toString(), 0, MAX_CASE_STATES_LENGTH);
    }
}
