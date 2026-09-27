package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.friend.aspect.CheckUserStatus;
import cn.nuonuoya.friend.dto.AppealCreateDTO;
import cn.nuonuoya.friend.service.AppealService;
import cn.nuonuoya.friend.vo.AppealQuotaVO;
import cn.nuonuoya.friend.vo.AppealReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 提交申诉控制器（学员端）
@RestController
@RequestMapping("/appeal")
@Tag(name = "提交申诉API")
public class AppealController extends BaseController {

    @Autowired
    private AppealService appealService;

    /** 查询今日 AI 初审与申诉剩余次数 */
    @GetMapping("/quota")
    @Operation(summary = "申诉剩余次数", description = "AI 初审每天 10 次，正式申诉每天 5 次")
    public OJResult<AppealQuotaVO> quota() {
        return OJResult.ok(appealService.getQuota());
    }

    /** 对一条未通过的提交发起 AI 初审 */
    @CheckUserStatus
    @PostMapping("/review/{submitId}")
    @Operation(summary = "AI 初审", description = "AI 认为判题可能有误时，1 小时内可以提交正式申诉")
    public OJResult<AppealReviewVO> review(@PathVariable("submitId") Long submitId) {
        return OJResult.ok(appealService.review(submitId));
    }

    /** 初审放行后提交正式申诉 */
    @CheckUserStatus
    @PostMapping
    @Operation(summary = "提交申诉", description = "须先通过 AI 初审，填写理由，每条提交只能申诉一次")
    public OJResult<Void> create(@Validated @RequestBody AppealCreateDTO createDTO) {
        appealService.create(createDTO);
        return OJResult.ok();
    }
}
