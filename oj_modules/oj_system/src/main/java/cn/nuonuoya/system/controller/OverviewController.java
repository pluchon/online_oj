package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.service.OverviewService;
import cn.nuonuoya.system.vo.OverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 数据概览控制器
@RestController
@RequestMapping("/overview")
@Tag(name = "数据概览API")
public class OverviewController extends BaseController {

    @Autowired
    private OverviewService overviewService;

    /** 查询数据概览 */
    @GetMapping
    @Operation(summary = "数据概览", description = "今日与近 7 天的提交数、通过率、活跃用户，近 7 天趋势，难题榜，最近一场竞赛的参与情况")
    public OJResult<OverviewVO> overview() {
        return OJResult.ok(overviewService.getOverview());
    }
}
