package cn.nuonuoya.ai.controller;

import cn.nuonuoya.ai.service.AiQuestionService;
import cn.nuonuoya.api.ai.api.AiInternalApi;
import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiQuestionDraftDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// AI 服务内部接口控制器（不经网关暴露）
@RestController
public class AiInternalController implements AiInternalApi {

    @Autowired
    private AiQuestionService aiQuestionService;

    /** 根据一句话描述生成题面草稿 */
    @Override
    public AiQuestionDraftVO generateQuestionDraft(@Valid @RequestBody AiQuestionDraftDTO draftDTO) {
        return aiQuestionService.generateQuestionDraft(draftDTO);
    }

    /** 根据题面与主函数生成测试用例输入 */
    @Override
    public AiCaseInputVO generateCaseInputs(@Valid @RequestBody AiCaseInputDTO caseInputDTO) {
        return aiQuestionService.generateCaseInputs(caseInputDTO);
    }
}
