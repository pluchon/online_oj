package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.system.dto.AppealHandleDTO;
import cn.nuonuoya.system.dto.AppealQueryDTO;
import cn.nuonuoya.system.service.AppealService;
import cn.nuonuoya.system.vo.AppealDetailVO;
import cn.nuonuoya.system.vo.AppealVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 申诉管理控制器
@Validated
@RestController
@RequestMapping("/appeal")
@Tag(name = "申诉管理API")
public class AppealController extends BaseController {

    @Autowired
    private AppealService appealService;

    /** 分页查询申诉 */
    @GetMapping
    @Operation(summary = "申诉列表", description = "按用户ID、题目名称、最近天数筛选，按申诉时间倒序分页")
    public TableDataResult<AppealVO> list(@Validated AppealQueryDTO queryDTO) {
        return appealService.list(queryDTO);
    }

    /** 查询申诉详情 */
    @GetMapping("/{appealId}")
    @Operation(summary = "申诉详情", description = "含申诉理由、AI 初审分析、提交代码与逐用例输入、预期和实际输出")
    public OJResult<AppealDetailVO> detail(@PathVariable("appealId") Long appealId) {
        return OJResult.ok(appealService.getDetail(appealId));
    }

    /** 裁定申诉 */
    @PutMapping("/{appealId}/handle")
    @Operation(summary = "裁定申诉", description = "存疑、通过（改判为通过并通知学员）、不通过（驳回并通知学员）；通过与不通过后不能再改")
    public OJResult<Void> handle(@PathVariable("appealId") Long appealId, @Validated @RequestBody AppealHandleDTO handleDTO) {
        appealService.handle(appealId, handleDTO);
        return OJResult.ok();
    }
}
