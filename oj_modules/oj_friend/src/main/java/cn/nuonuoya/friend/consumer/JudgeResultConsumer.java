package cn.nuonuoya.friend.consumer;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.judge.constants.JudgeMqConstants;
import cn.nuonuoya.api.judge.vo.JudgeCaseResultVO;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// 判题结果消息队列监听消费者
@Slf4j
@Component
public class JudgeResultConsumer {

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

    // 注入提交记录持久化 Mapper
    @Autowired
    private UserSubmitMapper userSubmitMapper;

    // 监听判题结果队列并更新提交记录
    @RabbitListener(queues = JudgeMqConstants.JUDGE_RESULT_QUEUE)
    public void onJudgeResult(JudgeResultVO resultVO) {
        if (resultVO == null || resultVO.getSubmitId() == null) {
            log.warn("收到空判题结果通知，忽略处理");
            return;
        }

        Long submitId = resultVO.getSubmitId();
        log.info("从 RabbitMQ 收到判题结果通知: submitId = {}, pass = {}, score = {}, status = {}",
                submitId, resultVO.getPass(), resultVO.getScore(), resultVO.getStatus());

        TbUserSubmit submit = new TbUserSubmit();
        submit.setSubmitId(submitId);
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

        userSubmitMapper.updateById(submit);
        log.info("提交记录更新成功: submitId = {}", submitId);
    }

    // 将逐用例结果编码为状态串（1: 通过 0: 未通过 -: 未执行）
    private String buildCaseStates(JudgeResultVO resultVO) {
        List<JudgeCaseResultVO> caseResults = resultVO.getCaseResults();
        if (caseResults == null || caseResults.isEmpty()) {
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
