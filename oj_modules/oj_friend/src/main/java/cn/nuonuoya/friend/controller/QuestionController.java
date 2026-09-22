package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.aspect.CheckUserStatus;
import cn.nuonuoya.friend.dto.QuestionQueryDTO;
import cn.nuonuoya.friend.dto.QuestionRunDTO;
import cn.nuonuoya.friend.dto.SubmitHistoryQueryDTO;
import cn.nuonuoya.friend.dto.UserSubmitDTO;
import cn.nuonuoya.friend.service.QuestionService;
import cn.nuonuoya.friend.service.UserSubmitService;
import cn.nuonuoya.friend.vo.QuestionPreNextVO;
import cn.nuonuoya.friend.vo.QuestionRunResultVO;
import cn.nuonuoya.friend.vo.QuestionStatsVO;
import cn.nuonuoya.friend.vo.QuestionVO;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;
import cn.nuonuoya.friend.vo.UserSubmitResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// C端题目搜索与列表控制器
@Validated
@RestController
@RequestMapping("/question")
@Tag(name = "C端题目API")
public class QuestionController extends BaseController {

    // 注入题目业务服务
    @Autowired
    private QuestionService questionService;

    // 注入代码提交评测服务
    @Autowired
    private UserSubmitService userSubmitService;

    /** 分页检索题目列表 */
    @GetMapping
    @Operation(summary = "题目列表检索", description = "支持关键字(标题/内容)最少切分模糊检索、难度筛选与分页")
    public TableDataResult<QuestionVO> list(QuestionQueryDTO queryDTO) {
        return questionService.search(queryDTO);
    }

    /** 获取题目详情 */
    @GetMapping("/{questionId}")
    @Operation(summary = "题目详情", description = "根据题目ID获取详细信息")
    public OJResult<QuestionVO> detail(@PathVariable("questionId") Long questionId) {
        QuestionVO vo = questionService.getDetail(questionId);
        return OJResult.ok(vo);
    }

    /** 获取题库做题统计信息 */
    @GetMapping("/stats")
    @Operation(summary = "题目统计信息", description = "获取题库总题数及当前用户的已攻克、尝试中题目统计")
    public OJResult<QuestionStatsVO> stats() {
        QuestionStatsVO vo = questionService.getStats();
        return OJResult.ok(vo);
    }

    /** 相似题推荐（需登录，排除已通过的题） */
    @GetMapping("/{questionId}/similar")
    @Operation(summary = "相似题推荐", description = "以题目向量检索相似题，排除当前题与当前用户已通过的题；题目尚无向量时返回空列表")
    public OJResult<List<QuestionVO>> similar(@PathVariable("questionId") Long questionId) {
        return OJResult.ok(questionService.listSimilar(questionId));
    }

    /** 获取上一题与下一题ID */
    @GetMapping("/{questionId}/neighbors")
    @Operation(summary = "题目导航", description = "根据当前题目ID与可选竞赛ID获取上一题与下一题ID")
    public OJResult<QuestionPreNextVO> preAndNext(
            @PathVariable("questionId") Long questionId,
            @RequestParam(value = "examId", required = false) Long examId) {
        QuestionPreNextVO vo = questionService.getPreAndNext(questionId, examId);
        return OJResult.ok(vo);
    }

    /** 获取首道题目ID */
    @GetMapping("/first")
    @Operation(summary = "获取首题ID", description = "获取普通题库或指定竞赛的首道题目ID")
    public OJResult<String> first(@RequestParam(value = "examId", required = false) Long examId) {
        Long firstId = questionService.getFirstQuestionId(examId);
        return OJResult.ok(firstId == null ? null : String.valueOf(firstId));
    }

    /** 用户提交代码并进行评测 */
    @CheckUserStatus
    @PostMapping("/{questionId}/submissions")
    @Operation(summary = "提交代码评测", description = "用户在答题工作台提交代码，执行入库与评测")
    public OJResult<UserSubmitResultVO> submit(@PathVariable("questionId") Long questionId,
                                               @RequestBody @Validated UserSubmitDTO submitDTO) {
        submitDTO.setQuestionId(questionId);
        UserSubmitResultVO vo = userSubmitService.submit(submitDTO);
        return OJResult.ok(vo);
    }

    /** 运行公开示例用例 */
    @CheckUserStatus
    @PostMapping("/{questionId}/run")
    @Operation(summary = "运行示例用例", description = "同步执行题目公开示例，不落库、不计分")
    public OJResult<QuestionRunResultVO> run(@PathVariable("questionId") Long questionId,
                                             @RequestBody @Validated QuestionRunDTO runDTO) {
        runDTO.setQuestionId(questionId);
        QuestionRunResultVO vo = userSubmitService.run(runDTO);
        return OJResult.ok(vo);
    }

    /** 分页查询本人本题提交记录 */
    @GetMapping("/{questionId}/submissions")
    @Operation(summary = "本题提交记录", description = "按提交时间倒序分页返回当前用户本题的提交记录")
    public TableDataResult<SubmitHistoryVO> submitHistory(@PathVariable("questionId") Long questionId,
                                                          @Validated SubmitHistoryQueryDTO queryDTO) {
        queryDTO.setQuestionId(questionId);
        return userSubmitService.listHistory(queryDTO);
    }

    /** 查询用户代码评测结果 */
    @GetMapping("/submissions/{submitId}")
    @Operation(summary = "查询评测结果", description = "根据提交ID获取最新评测状态与结果")
    public OJResult<UserSubmitResultVO> getSubmitResult(@PathVariable("submitId") Long submitId) {
        UserSubmitResultVO vo = userSubmitService.getSubmitResult(submitId);
        return OJResult.ok(vo);
    }
}
