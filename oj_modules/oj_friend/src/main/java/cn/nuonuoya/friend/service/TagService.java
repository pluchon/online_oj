package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.vo.QuestionTagVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 题目标签查询服务（标签由管理端维护，C端只读）
public interface TagService {

    // 查询全部标签（题库筛选用，按分类排序）
    List<QuestionTagVO> listTags();

    // 批量查询题目的标签（按题目ID分组，组内按添加先后）
    Map<Long, List<QuestionTagVO>> mapQuestionTags(Collection<Long> questionIds);

    // 批量查询题目的标签ID（写入ES索引用）
    Map<Long, List<Long>> mapQuestionTagIds(Collection<Long> questionIds);

    // 查询某分类下的全部标签ID
    Set<Long> listTagIdsByCategory(Integer category);

    // 查询带任一指定标签的题目ID
    Set<Long> listQuestionIdsByTags(Collection<Long> tagIds);
}
