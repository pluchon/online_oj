package cn.nuonuoya.system.client;

import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiQuestionDraftDTO;
import cn.nuonuoya.api.ai.dto.AiSolutionDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import cn.nuonuoya.api.ai.vo.AiSolutionVO;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

// AI 服务调用边界：远程失败统一转换为明确的业务错误码，不返回伪造结果
@Slf4j
@Component
public class AiClient {

    @Autowired
    private AiFeignClient aiFeignClient;

    // 生成题面草稿
    public AiQuestionDraftVO generateQuestionDraft(AiQuestionDraftDTO draftDTO) {
        return call("题面草稿", () -> aiFeignClient.generateQuestionDraft(draftDTO));
    }

    // 生成测试用例输入
    public AiCaseInputVO generateCaseInputs(AiCaseInputDTO caseInputDTO) {
        return call("用例输入", () -> aiFeignClient.generateCaseInputs(caseInputDTO));
    }

    // 生成解法示例
    public AiSolutionVO generateSolution(AiSolutionDTO solutionDTO) {
        return call("解法示例", () -> aiFeignClient.generateSolution(solutionDTO));
    }

    // 执行远程调用：参数错误返回参数校验失败，其余失败（含超时、服务不可用、空结果）返回 AI 服务繁忙
    private <T> T call(String scene, Supplier<T> action) {
        try {
            T result = action.get();
            if (result != null) {
                return result;
            }
            log.warn("AI 服务返回空结果, scene = {}", scene);
        } catch (FeignException.BadRequest e) {
            log.warn("AI 服务参数校验失败, scene = {}", scene);
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        } catch (Exception e) {
            log.error("调用 AI 服务失败, scene = {}, error = {}", scene, e.getMessage());
        }
        throw new ServiceException(ResultCode.FAILED_AI_BUSY);
    }
}
