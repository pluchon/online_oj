package cn.nuonuoya.friend.consumer;

import cn.nuonuoya.api.judge.constants.JudgeMqConstants;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.friend.service.UserSubmitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 判题结果消息监听器（只负责接收消息，回写逻辑由提交服务处理）
@Slf4j
@Component
public class JudgeResultConsumer {

    @Autowired
    private UserSubmitService userSubmitService;

    // 监听判题结果队列并回写提交记录
    @RabbitListener(queues = JudgeMqConstants.JUDGE_RESULT_QUEUE)
    public void onJudgeResult(JudgeResultVO resultVO) {
        if (resultVO == null || resultVO.getSubmitId() == null) {
            log.warn("收到空判题结果通知，忽略处理");
            return;
        }
        log.info("收到判题结果: submitId = {}, pass = {}, score = {}, status = {}",
                resultVO.getSubmitId(), resultVO.getPass(), resultVO.getScore(), resultVO.getStatus());
        userSubmitService.saveJudgeResult(resultVO);
    }
}
