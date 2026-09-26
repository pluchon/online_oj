package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.friend.dto.FriendSubmitQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.api.friend.vo.FriendRejudgeResultVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitDetailVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitPageVO;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.client.FriendSubmitClient;
import cn.nuonuoya.system.converter.SubmitConverter;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.domain.TbUser;
import cn.nuonuoya.system.dto.SubmitQueryDTO;
import cn.nuonuoya.system.mapper.ExamMapper;
import cn.nuonuoya.system.mapper.QuestionCaseMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.mapper.UserMapper;
import cn.nuonuoya.system.service.SubmitService;
import cn.nuonuoya.system.vo.RejudgePreviewVO;
import cn.nuonuoya.system.vo.SubmitDetailVO;
import cn.nuonuoya.system.vo.SubmitVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// 提交记录管理业务实现（提交数据来自 oj-friend，昵称、题目与竞赛标题由本服务按自己的表补充）
@Slf4j
@Service
public class SubmitServiceImpl implements SubmitService {

    @Autowired
    private FriendSubmitClient friendSubmitClient;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionCaseMapper questionCaseMapper;

    // 分页查询提交记录：昵称条件先在用户表解析为用户ID，再交给 oj-friend 查询
    @Override
    public TableDataResult<SubmitVO> list(SubmitQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new SubmitQueryDTO();
        }
        FriendSubmitQueryDTO friendQuery = SubmitConverter.toFriendQuery(queryDTO);
        if (StrUtil.isNotBlank(queryDTO.getNickName())) {
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<TbUser>()
                            .select(TbUser::getUserId)
                            .like(TbUser::getNickName, queryDTO.getNickName().trim()))
                    .stream()
                    .map(TbUser::getUserId)
                    .toList();
            if (userIds.isEmpty()) {
                return TableDataResult.empty();
            }
            friendQuery.setUserIds(userIds);
        }

        FriendSubmitPageVO page = friendSubmitClient.listSubmits(friendQuery);
        if (page == null || CollUtil.isEmpty(page.getRows())) {
            return TableDataResult.empty();
        }
        List<SubmitVO> voList = SubmitConverter.toVOList(page.getRows());
        fillNames(voList);
        return TableDataResult.success(voList, page.getTotal());
    }

    // 查询单条提交详情，并按首个未通过用例ID补充判题输入与预期输出
    @Override
    public SubmitDetailVO getDetail(Long submitId) {
        FriendSubmitDetailVO detail = friendSubmitClient.getSubmit(submitId);
        if (detail == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        SubmitDetailVO vo = SubmitConverter.toDetailVO(detail);
        if (detail.getFailCaseId() != null) {
            TbQuestionCase failCase = questionCaseMapper.selectById(detail.getFailCaseId());
            if (failCase != null) {
                vo.setFailCaseInput(failCase.getJudgeInput());
                vo.setFailCaseExpected(failCase.getJudgeOutput());
            }
        }
        fillNames(List.of(vo));
        return vo;
    }

    // 预览按题重判的影响范围
    @Override
    public RejudgePreviewVO previewRejudge(Long questionId) {
        checkQuestionExists(questionId);
        FriendRejudgePreviewVO preview = friendSubmitClient.previewRejudge(questionId);
        if (preview == null) {
            throw new ServiceException(ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE);
        }
        return SubmitConverter.toPreviewVO(preview);
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
        if (result == null) {
            throw new ServiceException(ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE);
        }
        if (Boolean.TRUE.equals(result.getDeliverFailed())) {
            throw new ServiceException(ResultCode.FAILED_REJUDGE_DELIVER);
        }
        return result.getQueuedCount() == null ? 0 : result.getQueuedCount();
    }

    // 校验题目存在
    private void checkQuestionExists(Long questionId) {
        if (questionId == null || questionMapper.selectById(questionId) == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
    }

    // 批量补充用户昵称、题目标题与竞赛标题，避免 N+1
    private void fillNames(List<? extends SubmitVO> voList) {
        Set<Long> userIds = collectIds(voList, SubmitVO::getUserId);
        Set<Long> questionIds = collectIds(voList, SubmitVO::getQuestionId);
        Set<Long> examIds = collectIds(voList, SubmitVO::getExamId);
        Map<Long, String> nickNames = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectList(new LambdaQueryWrapper<TbUser>()
                        .select(TbUser::getUserId, TbUser::getNickName)
                        .in(TbUser::getUserId, userIds))
                .stream()
                .filter(user -> user.getNickName() != null)
                .collect(Collectors.toMap(TbUser::getUserId, TbUser::getNickName));
        Map<Long, String> questionTitles = questionIds.isEmpty() ? Collections.emptyMap()
                : questionMapper.selectList(new LambdaQueryWrapper<TbQuestion>()
                        .select(TbQuestion::getQuestionId, TbQuestion::getTitle)
                        .in(TbQuestion::getQuestionId, questionIds))
                .stream()
                .collect(Collectors.toMap(TbQuestion::getQuestionId, TbQuestion::getTitle));
        Map<Long, String> examTitles = examIds.isEmpty() ? Collections.emptyMap()
                : examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                        .select(TbExam::getExamId, TbExam::getTitle)
                        .in(TbExam::getExamId, examIds))
                .stream()
                .collect(Collectors.toMap(TbExam::getExamId, TbExam::getTitle));
        for (SubmitVO vo : voList) {
            vo.setNickName(nickNames.get(vo.getUserId()));
            vo.setQuestionTitle(questionTitles.get(vo.getQuestionId()));
            vo.setExamTitle(vo.getExamId() == null ? null : examTitles.get(vo.getExamId()));
        }
    }

    // 收集列表中非空的ID
    private Set<Long> collectIds(List<? extends SubmitVO> voList, Function<SubmitVO, Long> getter) {
        return voList.stream().map(getter).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
