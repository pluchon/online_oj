package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.aspect.CheckUserStatus;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.service.ExamService;
import cn.nuonuoya.friend.vo.ExamRankVO;
import cn.nuonuoya.friend.vo.ExamStatsVO;
import cn.nuonuoya.friend.vo.ExamVO;
import cn.nuonuoya.friend.vo.UserExamVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// C端竞赛控制器
@Validated
@RestController
@RequestMapping("/exam")
@Tag(name = "C端竞赛API")
public class ExamController extends BaseController {

    @Autowired
    private ExamService examService;

    /** 分页查询竞赛列表（通用入口） */
    @GetMapping
    @Operation(summary = "竞赛列表", description = "支持按分类(未完赛/历史竞赛)、起止时间及标题筛选的分页查询")
    public TableDataResult<ExamVO> list(@Validated ExamQueryDTO queryDTO) {
        List<ExamVO> list = examService.list(queryDTO);
        return getTableData(list);
    }

    /** 查询指定竞赛详情 */
    @GetMapping("/{examId}")
    @Operation(summary = "竞赛详情", description = "根据竞赛ID查询详情与开赛状态")
    public OJResult<ExamVO> detail(@PathVariable("examId") Long examId) {
        ExamVO vo = examService.getExamDetail(examId);
        return OJResult.ok(vo);
    }

    /** 竞赛报名 */
    @CheckUserStatus
    @PostMapping("/{examId}/enrollment")
    @Operation(summary = "竞赛报名", description = "当前登录用户报名参加指定竞赛")
    public OJResult<Void> enroll(@PathVariable("examId") Long examId) {
        examService.enroll(examId);
        return OJResult.ok();
    }

    /** 竞赛状态统计 */
    @GetMapping("/stats")
    @Operation(summary = "竞赛状态统计", description = "mine 为 true 时只统计当前用户已报名的竞赛；不受列表筛选条件影响")
    public OJResult<ExamStatsVO> stats(@RequestParam(value = "mine", defaultValue = "false") boolean mine) {
        return OJResult.ok(examService.getStats(mine));
    }

    /** 分页查询我的竞赛列表 */
    @GetMapping("/mine")
    @Operation(summary = "我的竞赛列表", description = "分页查询当前登录用户报名的竞赛列表，支持类型、名称与时间区间筛选")
    public TableDataResult<UserExamVO> myExamList(@Validated ExamQueryDTO queryDTO) {
        List<UserExamVO> list = examService.getMyExamList(queryDTO);
        return getTableData(list);
    }

    /** 分页查询指定竞赛的选手排名榜单 */
    @GetMapping("/{examId}/rank")
    @Operation(summary = "竞赛排名列表", description = "分页查询指定竞赛的选手得分与排名榜单")
    public TableDataResult<ExamRankVO> rankList(@PathVariable("examId") Long examId, PageQuery pageQuery) {
        return examService.getExamRankList(examId, pageQuery);
    }
}
