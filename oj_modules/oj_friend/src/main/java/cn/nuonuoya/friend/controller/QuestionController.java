package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.dto.QuestionQueryDTO;
import cn.nuonuoya.friend.service.QuestionService;
import cn.nuonuoya.friend.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// C端题目搜索与列表控制器
@Validated
@RestController
@RequestMapping("/question")
@Tag(name = "C端题目API")
public class QuestionController extends BaseController {

    // 注入题目业务服务
    @Autowired
    private QuestionService questionService;

    /** 分页检索题目列表 */
    @GetMapping("/list")
    @Operation(summary = "题目列表检索", description = "支持关键字(标题/内容)最少切分模糊检索、难度筛选与分页")
    public TableDataResult<QuestionVO> list(QuestionQueryDTO queryDTO) {
        return questionService.search(queryDTO);
    }

    /** 获取题目详情 */
    @GetMapping("/detail")
    @Operation(summary = "题目详情", description = "根据题目ID获取详细信息")
    public OJResult<QuestionVO> detail(@RequestParam("questionId") Long questionId) {
        QuestionVO vo = questionService.getDetail(questionId);
        return OJResult.ok(vo);
    }

    /** 手动同步MySQL题目至ES索引 */
    @PostMapping("/sync")
    @Operation(summary = "同步题目至ES", description = "手动触发将MySQL题目全量写入ES索引")
    public OJResult<Integer> sync() {
        int count = questionService.syncAllQuestionsToEs();
        return OJResult.ok(count);
    }
}
