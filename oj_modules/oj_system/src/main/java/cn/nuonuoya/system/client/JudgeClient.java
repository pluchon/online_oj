package cn.nuonuoya.system.client;

import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.sentinel.SentinelGuard;
import cn.nuonuoya.system.constants.SentinelResources;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 判题服务调用边界（远程不可用、返回为空、被限流或熔断时返回 null，由调用方给出明确错误）
@Slf4j
@Component
public class JudgeClient {

    @Autowired
    private JudgeFeignClient judgeFeignClient;

    // 同步运行代码并返回逐用例结果，失败返回 null
    public JudgeResultVO run(JudgeRequestDTO requestDTO) {
        try {
            JudgeResultVO resultVO = SentinelGuard.call(SentinelResources.JUDGE_RUN, () -> judgeFeignClient.run(requestDTO));
            if (resultVO != null && resultVO.getStatus() != null) {
                return resultVO;
            }
            log.warn("判题服务返回空结果");
        } catch (BlockException e) {
            log.warn("判题调用被限流或熔断, rule = {}", e.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("调用判题服务失败, error = {}", e.getMessage());
        }
        return null;
    }
}
