package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.dto.OverviewExamQueryDTO;
import cn.nuonuoya.system.dto.OverviewTrendQueryDTO;
import cn.nuonuoya.system.service.OverviewService;
import cn.nuonuoya.system.vo.OverviewExamSummaryVO;
import cn.nuonuoya.system.vo.OverviewTrendVO;
import cn.nuonuoya.system.vo.OverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 数据概览控制器
@Validated
@RestController
@RequestMapping("/overview")
@Tag(name = "数据概览API")
public class OverviewController extends BaseController {

    @Autowired
    private OverviewService overviewService;

    /** 查询数据概览 */
    @GetMapping
    @Operation(summary = "数据概览", description = "今日与近 7 天的提交数、通过率、活跃用户，难题榜")
    public OJResult<OverviewVO> overview() {
        return OJResult.ok(overviewService.getOverview());
    }

    /** 查询近 N 天的每日提交趋势 */
    @GetMapping("/trend")
    @Operation(summary = "提交趋势", description = "近 N 天（含今日，1 ~ 30）每日提交数、通过数与通过率，没有提交的日子为 0")
    public OJResult<List<OverviewTrendVO>> trend(@Validated OverviewTrendQueryDTO queryDTO) {
        return OJResult.ok(overviewService.getTrend(queryDTO.getDays()));
    }

    /** 查询近 N 天内进行过的竞赛统计 */
    @GetMapping("/exam")
    @Operation(summary = "竞赛统计", description = "近 N 天（含今日，1 ~ 30）内进行过的竞赛：去重报名人数、参赛人数与参赛率，竞赛列表按开始时间倒序分页")
    public OJResult<OverviewExamSummaryVO> exam(@Validated OverviewExamQueryDTO queryDTO) {
        return OJResult.ok(overviewService.getExamSummary(queryDTO));
    }
}
