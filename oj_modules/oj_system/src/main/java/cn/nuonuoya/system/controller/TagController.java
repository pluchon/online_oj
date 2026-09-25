package cn.nuonuoya.system.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.system.dto.TagSaveDTO;
import cn.nuonuoya.system.service.TagService;
import cn.nuonuoya.system.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 题目标签控制器
@Validated
@RestController
@RequestMapping("/tag")
@Tag(name = "题目标签API")
public class TagController extends BaseController {

    @Autowired
    private TagService tagService;

    /** 查询全部标签 */
    @GetMapping
    @Operation(summary = "标签列表", description = "按分类返回全部标签及使用题目数")
    public OJResult<List<TagVO>> list() {
        return OJResult.ok(tagService.list());
    }

    /** 新增标签 */
    @PostMapping
    @Operation(summary = "新增标签", description = "标签名称在未删除的标签中唯一")
    public OJResult<Void> add(@Validated @RequestBody TagSaveDTO saveDTO) {
        return toResult(tagService.add(saveDTO));
    }

    /** 修改标签 */
    @PutMapping("/{tagId}")
    @Operation(summary = "修改标签", description = "修改标签名称与分类")
    public OJResult<Void> edit(@PathVariable("tagId") Long tagId, @Validated @RequestBody TagSaveDTO saveDTO) {
        return toResult(tagService.edit(tagId, saveDTO));
    }

    /** 删除标签 */
    @DeleteMapping("/{tagId}")
    @Operation(summary = "删除标签", description = "逻辑删除标签，并移除题目上的该标签")
    public OJResult<Void> delete(@PathVariable("tagId") Long tagId) {
        return toResult(tagService.delete(tagId));
    }
}
