package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.dto.QuestionAiCaseDTO;
import cn.nuonuoya.system.dto.QuestionAiDraftDTO;
import cn.nuonuoya.system.service.QuestionAiService;
import cn.nuonuoya.system.vo.QuestionAiCaseVO;
import cn.nuonuoya.system.vo.QuestionAiDraftVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI 辅助出题控制器（结果只用于回填表单，不落库）
@Validated
@RestController
@RequestMapping("/question/ai")
@Tag(name = "AI 辅助出题API")
public class QuestionAiController extends BaseController {

    @Autowired
    private QuestionAiService questionAiService;

    /** 根据一句话描述生成题面草稿 */
    @PostMapping("/draft")
    @Operation(summary = "生成题面草稿", description = "只回填表单，不保存")
    public OJResult<QuestionAiDraftVO> draft(@Validated @RequestBody QuestionAiDraftDTO draftDTO) {
        return OJResult.ok(questionAiService.generateDraft(draftDTO));
    }

    /** 生成测试用例预览 */
    @PostMapping("/cases")
    @Operation(summary = "生成测试用例", description = "模型生成输入，标程在判题沙箱中运行得到预期输出，不保存")
    public OJResult<QuestionAiCaseVO> cases(@Validated @RequestBody QuestionAiCaseDTO caseDTO) {
        return OJResult.ok(questionAiService.generateCases(caseDTO));
    }
}
