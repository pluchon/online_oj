package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.system.dto.ExamAddDTO;
import cn.nuonuoya.system.dto.ExamDTO;
import cn.nuonuoya.system.dto.ExamEditDTO;
import cn.nuonuoya.system.dto.ExamQuestionAddDTO;
import cn.nuonuoya.system.service.ExamService;
import cn.nuonuoya.system.vo.ExamDetailVO;
import cn.nuonuoya.system.vo.ExamVO;
import cn.nuonuoya.system.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 竞赛控制器
@Validated
@RestController
@RequestMapping("/exam")
@Tag(name = "竞赛API")
public class ExamController extends BaseController {

    @Autowired
    private ExamService examService;

    /** 分页查询竞赛列表 */
    @GetMapping("/list")
    @Operation(summary = "竞赛列表", description = "支持按时间范围与标题模糊筛选的分页查询")
    public TableDataResult<ExamVO> list(@Validated ExamDTO queryDTO) {
        List<ExamVO> list = examService.list(queryDTO);
        return getTableData(list);
    }

    /** 新增竞赛基本信息 */
    @PostMapping("/add")
    @Operation(summary = "新增竞赛基本信息", description = "校验入参并持久化竞赛基本信息，返回生成的竞赛ID")
    public OJResult<String> add(@Validated @RequestBody ExamAddDTO addDTO) {
        return OJResult.ok(examService.add(addDTO));
    }

    /** 获取竞赛详情 */
    @GetMapping("/detail")
    @Operation(summary = "竞赛详情", description = "根据竞赛ID查询详情信息")
    public OJResult<ExamDetailVO> detail(@NotNull(message = "竞赛ID不能为空") @RequestParam("examId") Long examId) {
        return OJResult.ok(examService.getDetail(examId));
    }

    /** 编辑竞赛基本信息 */
    @PutMapping("/edit")
    @Operation(summary = "编辑竞赛基本信息", description = "校验入参并更新竞赛基本信息")
    public OJResult<Void> edit(@Validated @RequestBody ExamEditDTO editDTO) {
        return toResult(examService.edit(editDTO));
    }

    /** 删除竞赛 */
    @DeleteMapping("/{examId}")
    @Operation(summary = "删除竞赛", description = "根据竞赛ID删除竞赛数据及关联题目")
    public OJResult<Void> delete(@NotNull(message = "竞赛ID不能为空") @PathVariable("examId") Long examId) {
        return toResult(examService.delete(examId));
    }

    /** 发布竞赛 */
    @PutMapping("/publish/{examId}")
    @Operation(summary = "发布竞赛", description = "校验题目数量并发布竞赛")
    public OJResult<Void> publish(@NotNull(message = "竞赛ID不能为空") @PathVariable("examId") Long examId) {
        return toResult(examService.publish(examId));
    }

    /** 撤销发布竞赛 */
    @PutMapping("/cancel-publish/{examId}")
    @Operation(summary = "撤销发布竞赛", description = "将竞赛恢复为未发布状态")
    public OJResult<Void> cancelPublish(@NotNull(message = "竞赛ID不能为空") @PathVariable("examId") Long examId) {
        return toResult(examService.cancelPublish(examId));
    }

    /** 绑定题目到竞赛 */
    @PostMapping("/question/add")
    @Operation(summary = "绑定题目到竞赛", description = "将选中的题目关联到指定竞赛")
    public OJResult<Void> addQuestion(@Validated @RequestBody ExamQuestionAddDTO addDTO) {
        return toResult(examService.addQuestion(addDTO));
    }

    /** 查询竞赛关联题目列表 */
    @GetMapping("/question/list")
    @Operation(summary = "竞赛关联题目列表", description = "查询指定竞赛已绑定的题目列表")
    public OJResult<List<QuestionVO>> getQuestionList(@NotNull(message = "竞赛ID不能为空") @RequestParam("examId") Long examId) {
        return OJResult.ok(examService.getQuestionList(examId));
    }

    /** 移除竞赛关联题目 */
    @DeleteMapping("/question/{examId}/{questionId}")
    @Operation(summary = "移除竞赛关联题目", description = "从竞赛中移除指定题目")
    public OJResult<Void> deleteQuestion(@NotNull(message = "竞赛ID不能为空") @PathVariable("examId") Long examId,
                                         @NotNull(message = "题目ID不能为空") @PathVariable("questionId") Long questionId) {
        return toResult(examService.deleteQuestion(examId, questionId));
    }

    /** 预热同步竞赛缓存 */
    @PostMapping("/cache/sync")
    @Operation(summary = "预热同步竞赛缓存", description = "将数据库中已发布的竞赛全量同步到Redis缓存中")
    public OJResult<Void> syncCache() {
        examService.syncCache();
        return OJResult.ok();
    }
}
