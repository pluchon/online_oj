package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.service.SubmitService;
import cn.nuonuoya.system.vo.RejudgePreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 按题重判控制器（管理员修改题目用例后使用）
@RestController
@RequestMapping("/submit")
@Tag(name = "按题重判API")
public class SubmitController extends BaseController {

    @Autowired
    private SubmitService submitService;

    /** 预览按题重判的影响范围 */
    @GetMapping("/rejudge/{questionId}")
    @Operation(summary = "重判影响范围", description = "会重判的练习与未结算竞赛提交数，以及跳过的已结算与评测中提交数")
    public OJResult<RejudgePreviewVO> previewRejudge(@PathVariable("questionId") Long questionId) {
        return OJResult.ok(submitService.previewRejudge(questionId));
    }

    /** 按题重判 */
    @PostMapping("/rejudge/{questionId}")
    @Operation(summary = "按题重判", description = "练习与未结算竞赛的提交重新投递判题，返回本次投递条数")
    public OJResult<Integer> rejudge(@PathVariable("questionId") Long questionId) {
        return OJResult.ok(submitService.rejudge(questionId));
    }
}
