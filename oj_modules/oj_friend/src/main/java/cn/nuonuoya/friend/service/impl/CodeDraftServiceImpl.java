package cn.nuonuoya.friend.service.impl;

import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.converter.CodeDraftConverter;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbUserCodeDraft;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserCodeDraftMapper;
import cn.nuonuoya.friend.service.CodeDraftService;
import cn.nuonuoya.friend.vo.CodeDraftVO;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 做题代码草稿实现：每个用户每道题一份，保存即覆盖
@Service
public class CodeDraftServiceImpl implements CodeDraftService {

    @Autowired
    private UserCodeDraftMapper userCodeDraftMapper;

    @Autowired
    private QuestionMapper questionMapper;

    // 查询当前用户在该题的草稿
    @Override
    public CodeDraftVO getDraft(Long questionId) {
        TbUserCodeDraft draft = findDraft(requireUserId(), questionId);
        return draft == null ? null : CodeDraftConverter.toVO(draft);
    }

    // 保存草稿：不存在则新增，已存在则覆盖；并发新增时以唯一索引兜底改为覆盖
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CodeDraftVO saveDraft(Long questionId, String code) {
        Long userId = requireUserId();
        TbQuestion question = questionId == null ? null : questionMapper.selectById(questionId);
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        TbUserCodeDraft existing = findDraft(userId, questionId);
        if (existing == null) {
            TbUserCodeDraft created = new TbUserCodeDraft();
            created.setUserId(userId);
            created.setQuestionId(questionId);
            created.setCode(code);
            try {
                userCodeDraftMapper.insert(created);
                return CodeDraftConverter.toVO(created);
            } catch (DuplicateKeyException e) {
                existing = findDraft(userId, questionId);
            }
        }
        userCodeDraftMapper.update(null, new LambdaUpdateWrapper<TbUserCodeDraft>()
                .set(TbUserCodeDraft::getCode, code)
                .set(TbUserCodeDraft::getUpdateBy, userId)
                .set(TbUserCodeDraft::getUpdateTime, LocalDateTime.now())
                .eq(TbUserCodeDraft::getDraftId, existing.getDraftId()));
        return CodeDraftConverter.toVO(findDraft(userId, questionId));
    }

    // 读取指定用户在该题保存的代码
    @Override
    public String getSavedCode(Long userId, Long questionId) {
        TbUserCodeDraft draft = findDraft(userId, questionId);
        return draft == null ? null : draft.getCode();
    }

    // 查询草稿
    private TbUserCodeDraft findDraft(Long userId, Long questionId) {
        return userCodeDraftMapper.selectOne(new LambdaQueryWrapper<TbUserCodeDraft>()
                .eq(TbUserCodeDraft::getUserId, userId)
                .eq(TbUserCodeDraft::getQuestionId, questionId));
    }

    // 当前登录用户
    private Long requireUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        return userId;
    }
}
