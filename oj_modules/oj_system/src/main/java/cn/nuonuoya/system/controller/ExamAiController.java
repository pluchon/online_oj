package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.dto.ExamAiPlanDTO;
import cn.nuonuoya.system.service.ExamAiService;
import cn.nuonuoya.system.vo.ExamAiPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI 帮建竞赛控制器（结果只用于回填表单，不落库）
@Validated
@RestController
@RequestMapping("/exam/ai")
@Tag(name = "AI 帮建竞赛API")
public class ExamAiController extends BaseController {

    @Autowired
    private ExamAiService examAiService;

    /** 根据描述、难度倾向与题目数量生成竞赛名称和题目 */
    @PostMapping("/plan")
    @Operation(summary = "AI 帮建竞赛", description = "模型理解需求，混合检索候选后由模型挑题，校验补齐后由易到难返回；不保存")
    public OJResult<ExamAiPlanVO> plan(@Validated @RequestBody ExamAiPlanDTO planDTO) {
        return OJResult.ok(examAiService.plan(planDTO));
    }
}
