package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.service.ExamService;
import cn.nuonuoya.friend.vo.ExamVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /** 竞赛报名 */
    @org.springframework.web.bind.annotation.PostMapping("/enroll")
    @Operation(summary = "竞赛报名", description = "当前登录用户报名参加指定竞赛")
    public cn.nuonuoya.common.domain.OJResult<Void> enroll(@Validated @org.springframework.web.bind.annotation.RequestBody cn.nuonuoya.friend.dto.ExamEnrollDTO enrollDTO) {
        examService.enroll(enrollDTO);
        return cn.nuonuoya.common.domain.OJResult.ok();
    }

    /** 分页查询我的竞赛列表 */
    @GetMapping("/my/list")
    @Operation(summary = "我的竞赛列表", description = "分页查询当前登录用户报名的竞赛列表")
    public TableDataResult<cn.nuonuoya.friend.vo.UserExamVO> myExamList(cn.nuonuoya.common.domain.PageQuery pageQuery) {
        List<cn.nuonuoya.friend.vo.UserExamVO> list = examService.getMyExamList(pageQuery);
        return getTableData(list);
    }
}
