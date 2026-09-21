package cn.nuonuoya.judge.consumer;

import cn.nuonuoya.api.judge.constants.JudgeMqConstants;
import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.enums.JudgePassEnum;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.judge.service.JudgeSandboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 判题任务消息队列监听消费者
@Slf4j
@Component
public class JudgeTaskConsumer {

    // 注入沙箱判题执行服务
    @Autowired
    private JudgeSandboxService judgeSandboxService;

    // 注入 RabbitTemplate 用于回传评测结果
    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 监听判题任务队列并执行 Docker 沙箱评测
    @RabbitListener(queues = JudgeMqConstants.JUDGE_QUEUE)
    public void onJudgeTask(JudgeRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getSubmitId() == null) {
            log.warn("收到空判题任务请求，忽略处理");
            return;
        }

        Long submitId = requestDTO.getSubmitId();
        log.info("从 RabbitMQ 队列接收到判题任务: submitId = {}, questionId = {}, userId = {}",
                submitId, requestDTO.getQuestionId(), requestDTO.getUserId());

        JudgeResultVO resultVO;
        try {
            // 调用 Docker 沙箱容器池执行评测
            resultVO = judgeSandboxService.executeJudge(requestDTO);
        } catch (Exception e) {
            log.error("沙箱执行发生未捕获异常: submitId = {}, error = {}", submitId, e.getMessage(), e);
            resultVO = new JudgeResultVO();
            resultVO.setSubmitId(submitId);
            resultVO.setStatus(JudgeStatusEnum.SE.getCode());
            resultVO.setStatusDesc(JudgeStatusEnum.SE.getName());
            resultVO.setPass(JudgePassEnum.NOT_PASS.getCode());
            resultVO.setScore(0);
            resultVO.setExeMessage("系统评测异常: " + e.getMessage());
        }

        // 将评测结果异步回传至结果队列
        rabbitTemplate.convertAndSend(
                JudgeMqConstants.JUDGE_RESULT_EXCHANGE,
                JudgeMqConstants.JUDGE_RESULT_ROUTING_KEY,
                resultVO
        );
        log.info("判题完成并回传结果消息至 RabbitMQ: submitId = {}, status = {}, pass = {}",
                submitId, resultVO.getStatus(), resultVO.getPass());
    }
}
