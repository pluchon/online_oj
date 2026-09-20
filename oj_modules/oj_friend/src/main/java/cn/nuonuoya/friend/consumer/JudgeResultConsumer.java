package cn.nuonuoya.friend.consumer;

import cn.nuonuoya.api.judge.constants.JudgeMqConstants;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 判题结果消息队列监听消费者
@Slf4j
@Component
public class JudgeResultConsumer {

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
        submit.setPass(resultVO.getPass() != null ? resultVO.getPass() : 0);
        submit.setScore(resultVO.getScore() != null ? resultVO.getScore() : 0);
        submit.setExeMessage(resultVO.getExeMessage());
        submit.setUpdateTime(LocalDateTime.now());

        userSubmitMapper.updateById(submit);
        log.info("提交记录更新成功: submitId = {}", submitId);
    }
}
