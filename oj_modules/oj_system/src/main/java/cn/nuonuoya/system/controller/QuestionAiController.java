package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.dto.QuestionAiCaseDTO;
import cn.nuonuoya.system.dto.QuestionAiDraftDTO;
import cn.nuonuoya.system.dto.QuestionAiEditorialDTO;
import cn.nuonuoya.system.dto.QuestionAiSolutionDTO;
import cn.nuonuoya.system.service.QuestionAiService;
import cn.nuonuoya.system.vo.QuestionAiCaseVO;
import cn.nuonuoya.system.vo.QuestionAiDraftVO;
import cn.nuonuoya.system.vo.QuestionAiEditorialVO;
import cn.nuonuoya.system.vo.QuestionAiSolutionVO;
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
    @Operation(summary = "生成测试用例", description = "模型生成输入，标程（未传时由 AI 生成）在判题沙箱中运行得到预期输出，不保存")
    public OJResult<QuestionAiCaseVO> cases(@Validated @RequestBody QuestionAiCaseDTO caseDTO) {
        return OJResult.ok(questionAiService.generateCases(caseDTO));
    }

    /** 生成解法示例 */
    @PostMapping("/solution")
    @Operation(summary = "生成解法示例", description = "生成常见解法用于参考与生成用例，不保存")
    public OJResult<QuestionAiSolutionVO> solution(@Validated @RequestBody QuestionAiSolutionDTO solutionDTO) {
        return OJResult.ok(questionAiService.generateSolution(solutionDTO));
    }

    /** 生成题解草稿 */
    @PostMapping("/editorial")
    @Operation(summary = "生成题解草稿", description = "生成思路、复杂度与代码组成的 Markdown 题解，只回填编辑框，随题目保存")
    public OJResult<QuestionAiEditorialVO> editorial(@Validated @RequestBody QuestionAiEditorialDTO editorialDTO) {
        return OJResult.ok(questionAiService.generateEditorial(editorialDTO));
    }
}
