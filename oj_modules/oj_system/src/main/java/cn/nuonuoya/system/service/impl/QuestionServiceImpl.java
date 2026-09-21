package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.client.FriendQuestionClient;
import cn.nuonuoya.system.converter.QuestionConverter;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.dto.QuestionAddDTO;
import cn.nuonuoya.system.dto.QuestionDTO;
import cn.nuonuoya.system.dto.QuestionEditDTO;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.service.QuestionService;
import cn.nuonuoya.system.utils.TransactionUtils;
import cn.nuonuoya.system.vo.QuestionDetailVO;
import cn.nuonuoya.system.vo.QuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 题目业务实现类
@Slf4j
@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private FriendQuestionClient friendQuestionClient;

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
            // 通过业务枚举为视图对象补充难度描述文案
            for (QuestionVO vo : list) {
                vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(vo.getDifficulty()));
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
        TbQuestion question = QuestionConverter.toEntity(addDTO);
        int rows = questionMapper.insert(question);
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
        return QuestionConverter.toDetailVO(question);
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
        // 转换更新字段并入库
        TbQuestion question = QuestionConverter.toEntity(editDTO);
        int rows = questionMapper.updateById(question);
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
        notifyQuestionChanged(rows);
        return rows;
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
