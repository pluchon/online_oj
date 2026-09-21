package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.aspect.CheckUserStatus;
import cn.nuonuoya.friend.dto.ExamEnrollDTO;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.service.ExamService;
import cn.nuonuoya.friend.vo.ExamRankVO;
import cn.nuonuoya.friend.vo.ExamVO;
import cn.nuonuoya.friend.vo.UserExamVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    @GetMapping("/list")
    @Operation(summary = "竞赛列表", description = "支持按分类(未完赛/历史竞赛)、起止时间及标题筛选的分页查询")
    public TableDataResult<ExamVO> list(@Validated ExamQueryDTO queryDTO) {
        List<ExamVO> list = examService.list(queryDTO);
        return getTableData(list);
    }

    /** 分页查询未完赛竞赛列表 */
    @GetMapping("/unfinish/list")
    @Operation(summary = "未完赛竞赛列表", description = "查询当前未结束的竞赛列表")
    public TableDataResult<ExamVO> unfinishList(@Validated ExamQueryDTO queryDTO) {
        List<ExamVO> list = examService.getUnfinishList(queryDTO);
        return getTableData(list);
    }

    /** 分页查询历史竞赛列表 */
    @GetMapping("/history/list")
    @Operation(summary = "历史竞赛列表", description = "查询已结束的历史竞赛列表")
    public TableDataResult<ExamVO> historyList(@Validated ExamQueryDTO queryDTO) {
        List<ExamVO> list = examService.getHistoryList(queryDTO);
        return getTableData(list);
    }

    /** 查询指定竞赛详情 */
    @GetMapping("/detail")
    @Operation(summary = "竞赛详情", description = "根据竞赛ID查询详情与开赛状态")
    public OJResult<ExamVO> detail(@RequestParam("examId") Long examId) {
        ExamVO vo = examService.getExamDetail(examId);
        return OJResult.ok(vo);
    }

    /** 竞赛报名 */
    @CheckUserStatus
    @PostMapping("/enroll")
    @Operation(summary = "竞赛报名", description = "当前登录用户报名参加指定竞赛")
    public OJResult<Void> enroll(@RequestBody @Validated ExamEnrollDTO enrollDTO) {
        examService.enroll(enrollDTO);
        return OJResult.ok();
    }

    /** 分页查询我的竞赛列表 */
    @GetMapping("/my/list")
    @Operation(summary = "我的竞赛列表", description = "分页查询当前登录用户报名的竞赛列表，支持类型、名称与时间区间筛选")
    public TableDataResult<UserExamVO> myExamList(@Validated ExamQueryDTO queryDTO) {
        List<UserExamVO> list = examService.getMyExamList(queryDTO);
        return getTableData(list);
    }

    /** 分页查询指定竞赛的选手排名榜单 */
    @GetMapping("/rank/list")
    @Operation(summary = "竞赛排名列表", description = "分页查询指定竞赛的选手得分与排名榜单")
    public TableDataResult<ExamRankVO> rankList(@RequestParam("examId") Long examId, PageQuery pageQuery) {
        return examService.getExamRankList(examId, pageQuery);
    }

    /** 结算指定竞赛排名并发送战报通知 */
    @PostMapping("/rank/settle")
    @Operation(summary = "结算竞赛排名", description = "手动或定时触发结算指定竞赛排名并向选手发送战报通知")
    public OJResult<Void> settleRank(@RequestParam("examId") Long examId) {
        examService.settleExamRank(examId);
        return OJResult.ok();
    }
}
