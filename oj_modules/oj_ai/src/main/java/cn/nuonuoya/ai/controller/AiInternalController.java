package cn.nuonuoya.ai.controller;

import cn.nuonuoya.ai.service.AiExamService;
import cn.nuonuoya.ai.service.AiQuestionService;
import cn.nuonuoya.api.ai.api.AiInternalApi;
import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiEditorialDTO;
import cn.nuonuoya.api.ai.dto.AiExamIntentDTO;
import cn.nuonuoya.api.ai.dto.AiExamSelectDTO;
import cn.nuonuoya.api.ai.dto.AiQuestionDraftDTO;
import cn.nuonuoya.api.ai.dto.AiSolutionDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputVO;
import cn.nuonuoya.api.ai.vo.AiEditorialVO;
import cn.nuonuoya.api.ai.vo.AiExamIntentVO;
import cn.nuonuoya.api.ai.vo.AiExamSelectVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import cn.nuonuoya.api.ai.vo.AiSolutionVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// AI 服务内部接口控制器（不经网关暴露）
@RestController
public class AiInternalController implements AiInternalApi {

    @Autowired
    private AiQuestionService aiQuestionService;

    @Autowired
    private AiExamService aiExamService;

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

    /** 根据题面生成常见解法 */
    @Override
    public AiSolutionVO generateSolution(@Valid @RequestBody AiSolutionDTO solutionDTO) {
        return aiQuestionService.generateSolution(solutionDTO);
    }

    /** 根据题面生成题解草稿 */
    @Override
    public AiEditorialVO generateEditorial(@Valid @RequestBody AiEditorialDTO editorialDTO) {
        return aiQuestionService.generateEditorial(editorialDTO);
    }

    /** 理解竞赛描述 */
    @Override
    public AiExamIntentVO parseExamIntent(@Valid @RequestBody AiExamIntentDTO intentDTO) {
        return aiExamService.parseIntent(intentDTO);
    }

    /** 从候选中挑题 */
    @Override
    public AiExamSelectVO selectExamQuestions(@Valid @RequestBody AiExamSelectDTO selectDTO) {
        return aiExamService.selectQuestions(selectDTO);
    }
}
