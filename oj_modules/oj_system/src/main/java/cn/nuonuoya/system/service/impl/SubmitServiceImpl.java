package cn.nuonuoya.system.service.impl;

import cn.nuonuoya.api.friend.vo.FriendRejudgeResultVO;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.client.FriendSubmitClient;
import cn.nuonuoya.system.converter.SubmitConverter;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.mapper.QuestionCaseMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.service.SubmitService;
import cn.nuonuoya.system.vo.RejudgePreviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 按题重判业务实现（重判范围见 D-015，由 oj-friend 执行）
@Service
public class SubmitServiceImpl implements SubmitService {

    @Autowired
    private FriendSubmitClient friendSubmitClient;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionCaseMapper questionCaseMapper;

    // 预览按题重判的影响范围
    @Override
    public RejudgePreviewVO previewRejudge(Long questionId) {
        checkQuestionExists(questionId);
        return SubmitConverter.toPreviewVO(friendSubmitClient.previewRejudge(questionId));
    }

    // 按题重判：题目须存在且配置了用例；判题队列投递失败时返回 3502（已投递的会正常判完）
    @Override
    public int rejudge(Long questionId) {
        checkQuestionExists(questionId);
        Long caseCount = questionCaseMapper.selectCount(new LambdaQueryWrapper<TbQuestionCase>()
                .eq(TbQuestionCase::getQuestionId, questionId));
        if (caseCount == null || caseCount == 0) {
            throw new ServiceException(ResultCode.FAILED_QUESTION_NO_CASE);
        }
        FriendRejudgeResultVO result = friendSubmitClient.rejudge(questionId);
        if (Boolean.TRUE.equals(result.getDeliverFailed())) {
            throw new ServiceException(ResultCode.FAILED_REJUDGE_DELIVER);
        }
        return result.getQueuedCount();
    }

    // 校验题目存在
    private void checkQuestionExists(Long questionId) {
        if (questionId == null || questionMapper.selectById(questionId) == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
    }
}
