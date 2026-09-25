package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.mybatis.utils.TransactionUtils;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.client.FriendQuestionClient;
import cn.nuonuoya.system.converter.TagConverter;
import cn.nuonuoya.system.domain.TbQuestionTag;
import cn.nuonuoya.system.domain.TbTag;
import cn.nuonuoya.system.dto.TagSaveDTO;
import cn.nuonuoya.system.mapper.QuestionTagMapper;
import cn.nuonuoya.system.mapper.TagMapper;
import cn.nuonuoya.system.service.TagService;
import cn.nuonuoya.system.vo.QuestionTagVO;
import cn.nuonuoya.system.vo.TagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// 题目标签业务实现类
@Slf4j
@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private QuestionTagMapper questionTagMapper;

    @Autowired
    private FriendQuestionClient friendQuestionClient;

    // 查询全部标签：按分类、创建先后排序，并统计每个标签的使用题目数
    @Override
    public List<TagVO> list() {
        List<TbTag> tags = listAll();
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Integer> countMap = new HashMap<>();
        questionTagMapper.selectList(new LambdaQueryWrapper<TbQuestionTag>()
                        .select(TbQuestionTag::getTagId))
                .forEach(relation -> countMap.merge(relation.getTagId(), 1, Integer::sum));
        List<TagVO> voList = new ArrayList<>(tags.size());
        for (TbTag tag : tags) {
            voList.add(TagConverter.toVO(tag, countMap.getOrDefault(tag.getTagId(), 0)));
        }
        return voList;
    }

    // 新增标签（名称在未删除的标签中唯一）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(TagSaveDTO saveDTO) {
        TbTag tag = TagConverter.toEntity(saveDTO);
        checkNameUnique(tag.getTagName(), null);
        return tagMapper.insert(tag);
    }

    // 修改标签名称与分类（题目上显示的名称实时读取，不需要刷新索引）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int edit(Long tagId, TagSaveDTO saveDTO) {
        getExistingTag(tagId);
        TbTag tag = TagConverter.toEntity(saveDTO);
        tag.setTagId(tagId);
        checkNameUnique(tag.getTagName(), tagId);
        return tagMapper.updateById(tag);
    }

    // 删除标签并移除题目上的该标签；有题目受影响时，事务提交后通知C端刷新题目索引
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long tagId) {
        getExistingTag(tagId);
        int rows = tagMapper.deleteById(tagId);
        int relationRows = questionTagMapper.delete(new LambdaQueryWrapper<TbQuestionTag>()
                .eq(TbQuestionTag::getTagId, tagId));
        if (relationRows > 0) {
            notifyQuestionChanged();
        }
        return rows;
    }

    // 查询全部未删除的标签实体
    @Override
    public List<TbTag> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<TbTag>()
                .orderByAsc(TbTag::getCategory, TbTag::getTagId));
    }

    // 覆盖题目标签：去重后校验标签都存在，删掉不再选中的，补上新选中的
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceQuestionTags(Long questionId, List<Long> tagIds) {
        Set<Long> targetIds = new LinkedHashSet<>(CollUtil.emptyIfNull(tagIds));
        targetIds.remove(null);
        if (!targetIds.isEmpty()) {
            Long existCount = tagMapper.selectCount(new LambdaQueryWrapper<TbTag>()
                    .in(TbTag::getTagId, targetIds));
            if (existCount == null || existCount != targetIds.size()) {
                throw new ServiceException(ResultCode.FAILED_TAG_NOT_EXISTS);
            }
        }

        List<TbQuestionTag> current = questionTagMapper.selectList(new LambdaQueryWrapper<TbQuestionTag>()
                .eq(TbQuestionTag::getQuestionId, questionId));
        List<Long> staleIds = current.stream()
                .filter(relation -> !targetIds.contains(relation.getTagId()))
                .map(TbQuestionTag::getQuestionTagId)
                .collect(Collectors.toList());
        if (!staleIds.isEmpty()) {
            questionTagMapper.deleteByIds(staleIds);
        }

        Set<Long> currentTagIds = current.stream().map(TbQuestionTag::getTagId).collect(Collectors.toSet());
        for (Long tagId : targetIds) {
            if (currentTagIds.contains(tagId)) {
                continue;
            }
            TbQuestionTag relation = new TbQuestionTag();
            relation.setQuestionId(questionId);
            relation.setTagId(tagId);
            questionTagMapper.insert(relation);
        }
    }

    // 移除题目的全部标签
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeQuestionTags(Long questionId) {
        questionTagMapper.delete(new LambdaQueryWrapper<TbQuestionTag>()
                .eq(TbQuestionTag::getQuestionId, questionId));
    }

    // 查询单道题目的标签
    @Override
    public List<QuestionTagVO> listQuestionTags(Long questionId) {
        return mapQuestionTags(Collections.singletonList(questionId))
                .getOrDefault(questionId, Collections.emptyList());
    }

    // 批量查询题目标签：先取关联（按添加先后），再批量取标签名称
    @Override
    public Map<Long, List<QuestionTagVO>> mapQuestionTags(Collection<Long> questionIds) {
        if (CollUtil.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        List<TbQuestionTag> relations = questionTagMapper.selectList(new LambdaQueryWrapper<TbQuestionTag>()
                .select(TbQuestionTag::getQuestionId, TbQuestionTag::getTagId)
                .in(TbQuestionTag::getQuestionId, questionIds)
                .orderByAsc(TbQuestionTag::getQuestionTagId));
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
            result.computeIfAbsent(relation.getQuestionId(), k -> new ArrayList<>())
                    .add(TagConverter.toQuestionTagVO(tag));
        }
        return result;
    }

    // 校验标签存在
    private TbTag getExistingTag(Long tagId) {
        if (tagId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        TbTag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        return tag;
    }

    // 校验名称在未删除的标签中唯一（修改时排除自身）
    private void checkNameUnique(String tagName, Long excludeTagId) {
        Long count = tagMapper.selectCount(new LambdaQueryWrapper<TbTag>()
                .eq(TbTag::getTagName, tagName)
                .ne(Objects.nonNull(excludeTagId), TbTag::getTagId, excludeTagId));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_TAG_EXISTS);
        }
    }

    // 事务提交后通知C端刷新题目缓存与ES索引
    private void notifyQuestionChanged() {
        TransactionUtils.afterCommit(() -> {
            if (!friendQuestionClient.refreshQuestionData()) {
                log.warn("标签已删除但C端数据未刷新，C端按该标签筛选可能暂时仍有结果");
            }
        });
    }
}
