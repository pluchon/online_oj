package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.system.dto.SubmitQueryDTO;
import cn.nuonuoya.system.service.SubmitService;
import cn.nuonuoya.system.vo.RejudgePreviewVO;
import cn.nuonuoya.system.vo.SubmitDetailVO;
import cn.nuonuoya.system.vo.SubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 提交记录管理控制器
@Validated
@RestController
@RequestMapping("/submit")
@Tag(name = "提交记录API")
public class SubmitController extends BaseController {

    @Autowired
    private SubmitService submitService;

    /** 分页查询提交记录 */
    @GetMapping
    @Operation(summary = "提交记录列表", description = "按题目、用户昵称、判题结论、竞赛筛选，按提交时间倒序分页")
    public TableDataResult<SubmitVO> list(@Validated SubmitQueryDTO queryDTO) {
        return submitService.list(queryDTO);
    }

    /** 查询提交详情 */
    @GetMapping("/{submitId}")
    @Operation(summary = "提交详情", description = "含代码、执行回显与首个未通过用例")
    public OJResult<SubmitDetailVO> detail(@PathVariable("submitId") Long submitId) {
        return OJResult.ok(submitService.getDetail(submitId));
    }

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
