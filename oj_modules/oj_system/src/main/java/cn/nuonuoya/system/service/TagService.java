package cn.nuonuoya.system.service;

import cn.nuonuoya.system.domain.TbTag;
import cn.nuonuoya.system.dto.TagSaveDTO;
import cn.nuonuoya.system.vo.QuestionTagVO;
import cn.nuonuoya.system.vo.TagVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// 题目标签业务接口
public interface TagService {

    // 查询全部标签（附使用题目数）
    List<TagVO> list();

    // 新增标签
    int add(TagSaveDTO saveDTO);

    // 修改标签
    int edit(Long tagId, TagSaveDTO saveDTO);

    // 删除标签（同时移除题目上的该标签）
    int delete(Long tagId);

    // 查询全部未删除的标签实体（AI 出题建议标签用）
    List<TbTag> listAll();

    // 按提交的标签覆盖题目的标签（只增删有变化的部分）
    void replaceQuestionTags(Long questionId, List<Long> tagIds);

    // 移除题目的全部标签
    void removeQuestionTags(Long questionId);

    // 查询单道题目的标签
    List<QuestionTagVO> listQuestionTags(Long questionId);

    // 批量查询题目的标签（按题目ID分组）
    Map<Long, List<QuestionTagVO>> mapQuestionTags(Collection<Long> questionIds);
}
