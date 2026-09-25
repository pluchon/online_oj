package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.client.FriendQuestionClient;
import cn.nuonuoya.system.converter.QuestionConverter;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.dto.QuestionAddDTO;
import cn.nuonuoya.system.dto.QuestionCaseDTO;
import cn.nuonuoya.system.dto.QuestionDTO;
import cn.nuonuoya.system.dto.QuestionEditDTO;
import cn.nuonuoya.system.enums.QuestionCaseType;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.mapper.QuestionCaseMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.service.QuestionService;
import cn.nuonuoya.system.service.TagService;
import cn.nuonuoya.mybatis.utils.TransactionUtils;
import cn.nuonuoya.system.vo.QuestionDetailVO;
import cn.nuonuoya.system.vo.QuestionTagVO;
import cn.nuonuoya.system.vo.QuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 题目业务实现类
@Slf4j
@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private FriendQuestionClient friendQuestionClient;

    @Autowired
    private QuestionCaseMapper questionCaseMapper;

    @Autowired
    private TagService tagService;

    // 分页查询题目列表实现
    @Override
    public List<QuestionVO> list(QuestionDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new QuestionDTO();
        }
        // 开启 PageHelper 物理分页
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        // 执行联表查询，PageHelper 会自动拦截生成 COUNT 语句与物理 LIMIT 分页
        List<QuestionVO> list = questionMapper.selectQuestionList(queryDTO);
        if (CollUtil.isNotEmpty(list)) {
            // 补充难度描述文案，并批量装配当前页题目的标签
            Map<Long, List<QuestionTagVO>> tagMap = tagService.mapQuestionTags(
                    list.stream().map(QuestionVO::getQuestionId).toList());
            for (QuestionVO vo : list) {
                vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(vo.getDifficulty()));
                vo.setTags(tagMap.getOrDefault(vo.getQuestionId(), Collections.emptyList()));
            }
        }
        return list;
    }

    // 新增题目实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(QuestionAddDTO addDTO) {
        // 校验题目标题是否已存在
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<TbQuestion>()
                .eq(TbQuestion::getTitle, addDTO.getTitle().trim()));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_ALREADY_EXISTS);
        }
        checkCases(addDTO.getCases());
        TbQuestion question = QuestionConverter.toEntity(addDTO);
        int rows = questionMapper.insert(question);
        saveCases(question.getQuestionId(), addDTO.getCases());
        tagService.replaceQuestionTags(question.getQuestionId(), addDTO.getTagIds());
        notifyQuestionChanged(rows);
        return rows;
    }

    // 查询题目详情实现
    @Override
    public QuestionDetailVO getDetail(Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 根据ID查库获取题目实体
        TbQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        QuestionDetailVO vo = QuestionConverter.toDetailVO(question);
        vo.setCases(QuestionConverter.toCaseVOList(listCases(questionId)));
        vo.setTags(tagService.listQuestionTags(questionId));
        return vo;
    }

    // 修改题目实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int edit(QuestionEditDTO editDTO) {
        if (editDTO == null || editDTO.getQuestionId() == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验被修改题目是否存在
        TbQuestion oldQuestion = questionMapper.selectById(editDTO.getQuestionId());
        if (oldQuestion == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        // 校验题目标题是否与其他题目冲突（排除自身）
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<TbQuestion>()
                .eq(TbQuestion::getTitle, editDTO.getTitle().trim())
                .ne(TbQuestion::getQuestionId, editDTO.getQuestionId()));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_ALREADY_EXISTS);
        }
        checkCases(editDTO.getCases());
        // 转换更新字段并入库，用例整体替换（旧用例逻辑删除）
        TbQuestion question = QuestionConverter.toEntity(editDTO);
        int rows = questionMapper.updateById(question);
        questionCaseMapper.delete(new LambdaQueryWrapper<TbQuestionCase>()
                .eq(TbQuestionCase::getQuestionId, editDTO.getQuestionId()));
        saveCases(editDTO.getQuestionId(), editDTO.getCases());
        tagService.replaceQuestionTags(editDTO.getQuestionId(), editDTO.getTagIds());
        notifyQuestionChanged(rows);
        return rows;
    }

    // 删除题目实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验被删除题目是否存在
        TbQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        int rows = questionMapper.deleteById(questionId);
        questionCaseMapper.delete(new LambdaQueryWrapper<TbQuestionCase>()
                .eq(TbQuestionCase::getQuestionId, questionId));
        tagService.removeQuestionTags(questionId);
        notifyQuestionChanged(rows);
        return rows;
    }

    // 校验用例：至少包含一个公开示例（用于题面展示与运行）
    private void checkCases(List<QuestionCaseDTO> cases) {
        boolean hasSample = cases.stream()
                .anyMatch(c -> Objects.equals(c.getIsSample(), QuestionCaseType.SAMPLE.getValue()));
        if (!hasSample) {
            throw new ServiceException(ResultCode.FAILED_QUESTION_NO_SAMPLE);
        }
    }

    // 批量写入题目用例
    private void saveCases(Long questionId, List<QuestionCaseDTO> cases) {
        for (TbQuestionCase entity : QuestionConverter.toCaseEntities(questionId, cases)) {
            questionCaseMapper.insert(entity);
        }
    }

    // 查询题目的全部用例（公开示例在前）
    private List<TbQuestionCase> listCases(Long questionId) {
        return questionCaseMapper.selectList(new LambdaQueryWrapper<TbQuestionCase>()
                .eq(TbQuestionCase::getQuestionId, questionId)
                .orderByDesc(TbQuestionCase::getIsSample)
                .orderByAsc(TbQuestionCase::getSortOrder, TbQuestionCase::getCaseId));
    }

    // 题目有变更时，事务提交后通知C端刷新题目缓存与ES索引
    private void notifyQuestionChanged(int rows) {
        if (rows <= 0) {
            return;
        }
        TransactionUtils.afterCommit(() -> {
            if (!friendQuestionClient.refreshQuestionData()) {
                log.warn("题目已保存但C端数据未刷新，C端题库可能暂未更新");
            }
        });
    }
}
