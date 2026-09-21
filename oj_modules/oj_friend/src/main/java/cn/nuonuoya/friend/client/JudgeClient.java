package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 判题服务调用边界（远程失败时返回明确的系统错误结果，不伪装成功）
@Slf4j
@Component
public class JudgeClient {

    @Autowired
    private JudgeFeignClient judgeFeignClient;

    // 同步运行判题，远程不可用或返回为空时降级为系统错误
    public JudgeResultVO run(JudgeRequestDTO requestDTO) {
        try {
            JudgeResultVO resultVO = judgeFeignClient.run(requestDTO);
            if (resultVO != null && resultVO.getStatus() != null) {
                return resultVO;
            }
            log.warn("判题服务返回空结果, questionId = {}", requestDTO.getQuestionId());
        } catch (Exception e) {
            log.error("调用判题服务失败, questionId = {}, error = {}", requestDTO.getQuestionId(), e.getMessage());
        }
        JudgeResultVO fallback = new JudgeResultVO();
        fallback.setStatus(JudgeStatusEnum.SE.getCode());
        fallback.setStatusDesc(JudgeStatusEnum.SE.getName());
        fallback.setPass(0);
        fallback.setPassCount(0);
        fallback.setTotalCount(requestDTO.getCases() == null ? 0 : requestDTO.getCases().size());
        fallback.setExeMessage("判题服务暂不可用，请稍后重试");
        return fallback;
    }
}
