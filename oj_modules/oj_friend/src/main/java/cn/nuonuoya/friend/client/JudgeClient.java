package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.enums.JudgePassEnum;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.friend.constants.SentinelResources;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 判题服务调用边界（远程失败、被限流或熔断时返回明确的系统错误结果，不伪装成功）
@Slf4j
@Component
public class JudgeClient {

    @Autowired
    private JudgeFeignClient judgeFeignClient;

    // 远程不可用时的提示
    private static final String UNAVAILABLE_MESSAGE = "判题服务暂不可用，请稍后重试";

    // 被限流或熔断时的提示
    private static final String BUSY_MESSAGE = "判题服务繁忙，请稍后重试";

    // 同步运行判题，远程不可用、返回为空、被限流或熔断时降级为系统错误
    public JudgeResultVO run(JudgeRequestDTO requestDTO) {
        try {
            JudgeResultVO resultVO = SentinelGuard.call(SentinelResources.JUDGE_RUN, () -> judgeFeignClient.run(requestDTO));
            if (resultVO != null && resultVO.getStatus() != null) {
                return resultVO;
            }
            log.warn("判题服务返回空结果, questionId = {}", requestDTO.getQuestionId());
        } catch (BlockException e) {
            log.warn("判题调用被限流或熔断, questionId = {}, rule = {}", requestDTO.getQuestionId(), e.getClass().getSimpleName());
            return systemError(requestDTO, BUSY_MESSAGE);
        } catch (Exception e) {
            log.error("调用判题服务失败, questionId = {}, error = {}", requestDTO.getQuestionId(), e.getMessage());
        }
        return systemError(requestDTO, UNAVAILABLE_MESSAGE);
    }

    // 系统错误结果（未通过，逐用例为空）
    private JudgeResultVO systemError(JudgeRequestDTO requestDTO, String message) {
        JudgeResultVO fallback = new JudgeResultVO();
        fallback.setStatus(JudgeStatusEnum.SE.getCode());
        fallback.setStatusDesc(JudgeStatusEnum.SE.getName());
        fallback.setPass(JudgePassEnum.NOT_PASS.getCode());
        fallback.setPassCount(0);
        fallback.setTotalCount(requestDTO.getCases() == null ? 0 : requestDTO.getCases().size());
        fallback.setExeMessage(message);
        return fallback;
    }
}
