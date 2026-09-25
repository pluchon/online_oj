package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.friend.converter.TagConverter;
import cn.nuonuoya.friend.domain.TbQuestionTag;
import cn.nuonuoya.friend.domain.TbTag;
import cn.nuonuoya.friend.mapper.QuestionTagMapper;
import cn.nuonuoya.friend.mapper.TagMapper;
import cn.nuonuoya.friend.service.TagService;
import cn.nuonuoya.friend.vo.QuestionTagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// 题目标签查询服务实现
@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private QuestionTagMapper questionTagMapper;

    // 查询全部标签
    @Override
    public List<QuestionTagVO> listTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<TbTag>()
                        .orderByAsc(TbTag::getCategory, TbTag::getTagId))
                .stream()
                .map(TagConverter::toVO)
                .collect(Collectors.toList());
    }

    // 批量查询题目的标签：先取关联，再批量取标签（已删除的标签不返回）
    @Override
    public Map<Long, List<QuestionTagVO>> mapQuestionTags(Collection<Long> questionIds) {
        List<TbQuestionTag> relations = listRelations(questionIds);
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> tagIds = relations.stream().map(TbQuestionTag::getTagId).collect(Collectors.toSet());
        Map<Long, TbTag> tagMap = tagMapper.selectByIds(tagIds).stream()
                .collect(Collectors.toMap(TbTag::getTagId, Function.identity()));
        Map<Long, List<QuestionTagVO>> result = new HashMap<>();
        for (TbQuestionTag relation : relations) {
            TbTag tag = tagMap.get(relation.getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(relation.getQuestionId(), k -> new ArrayList<>()).add(TagConverter.toVO(tag));
        }
        return result;
    }

    // 批量查询题目的标签ID
    @Override
    public Map<Long, List<Long>> mapQuestionTagIds(Collection<Long> questionIds) {
        return listRelations(questionIds).stream()
                .collect(Collectors.groupingBy(TbQuestionTag::getQuestionId,
                        Collectors.mapping(TbQuestionTag::getTagId, Collectors.toList())));
    }

    // 查询某分类下的全部标签ID
    @Override
    public Set<Long> listTagIdsByCategory(Integer category) {
        return tagMapper.selectList(new LambdaQueryWrapper<TbTag>()
                        .select(TbTag::getTagId)
                        .eq(TbTag::getCategory, category))
                .stream()
                .map(TbTag::getTagId)
                .collect(Collectors.toSet());
    }

    // 查询带任一指定标签的题目ID
    @Override
    public Set<Long> listQuestionIdsByTags(Collection<Long> tagIds) {
        if (CollUtil.isEmpty(tagIds)) {
            return Collections.emptySet();
        }
        return questionTagMapper.selectList(new LambdaQueryWrapper<TbQuestionTag>()
                        .select(TbQuestionTag::getQuestionId)
                        .in(TbQuestionTag::getTagId, tagIds))
                .stream()
                .map(TbQuestionTag::getQuestionId)
                .collect(Collectors.toSet());
    }

    // 查询题目的标签关联（按添加先后）
    private List<TbQuestionTag> listRelations(Collection<Long> questionIds) {
        if (CollUtil.isEmpty(questionIds)) {
            return Collections.emptyList();
        }
        return questionTagMapper.selectList(new LambdaQueryWrapper<TbQuestionTag>()
                .select(TbQuestionTag::getQuestionId, TbQuestionTag::getTagId)
                .in(TbQuestionTag::getQuestionId, questionIds)
                .orderByAsc(TbQuestionTag::getQuestionTagId));
    }
}
