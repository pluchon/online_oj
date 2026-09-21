package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.system.dto.QuestionAddDTO;
import cn.nuonuoya.system.dto.QuestionDTO;
import cn.nuonuoya.system.dto.QuestionEditDTO;
import cn.nuonuoya.system.service.QuestionService;
import cn.nuonuoya.system.vo.QuestionDetailVO;
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

// 题目控制器
@Validated
@RestController
@RequestMapping("/question")
@Tag(name = "题目API")
public class QuestionController extends BaseController {

    @Autowired
    private QuestionService questionService;

    /** 分页查询题目列表 */
    @GetMapping
    @Operation(summary = "题目列表", description = "支持按难度与标题模糊筛选的分页查询")
    public TableDataResult<QuestionVO> list(@Validated QuestionDTO queryDTO) {
        List<QuestionVO> list = questionService.list(queryDTO);
        return getTableData(list);
    }

    /** 新增题目 */
    @PostMapping
    @Operation(summary = "新增题目", description = "校验入参并持久化题目数据")
    public OJResult<Void> add(@Validated @RequestBody QuestionAddDTO addDTO) {
        return toResult(questionService.add(addDTO));
    }

    /** 获取题目详情 */
    @GetMapping("/{questionId}")
    @Operation(summary = "题目详情", description = "根据题目ID查询详情信息")
    public OJResult<QuestionDetailVO> detail(@PathVariable("questionId") Long questionId) {
        return OJResult.ok(questionService.getDetail(questionId));
    }

    /** 编辑题目 */
    @PutMapping("/{questionId}")
    @Operation(summary = "编辑题目", description = "校验入参并更新题目信息")
    public OJResult<Void> edit(@PathVariable("questionId") Long questionId, @Validated @RequestBody QuestionEditDTO editDTO) {
        editDTO.setQuestionId(questionId);
        return toResult(questionService.edit(editDTO));
    }

    /** 删除题目 */
    @DeleteMapping("/{questionId}")
    @Operation(summary = "删除题目", description = "根据题目ID删除题目数据")
    public OJResult<Void> delete(@PathVariable("questionId") Long questionId) {
        return toResult(questionService.delete(questionId));
    }
}
