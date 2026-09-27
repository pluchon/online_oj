package cn.nuonuoya.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.friend.dto.FriendAppealHandleDTO;
import cn.nuonuoya.api.friend.dto.FriendAppealQueryDTO;
import cn.nuonuoya.api.friend.enums.AppealStatusEnum;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendAppealQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendAppealVO;
import cn.nuonuoya.api.friend.vo.FriendPageVO;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.utils.SecurityUtils;
import cn.nuonuoya.system.client.FriendAppealClient;
import cn.nuonuoya.system.converter.AppealConverter;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.domain.TbUser;
import cn.nuonuoya.system.dto.AppealHandleDTO;
import cn.nuonuoya.system.dto.AppealQueryDTO;
import cn.nuonuoya.system.mapper.QuestionCaseMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.mapper.UserMapper;
import cn.nuonuoya.system.service.AppealService;
import cn.nuonuoya.system.vo.AppealBaseVO;
import cn.nuonuoya.system.vo.AppealDetailVO;
import cn.nuonuoya.system.vo.AppealVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 申诉管理业务实现（规则见 D-017）
@Service
public class AppealServiceImpl implements AppealService {

    // 申诉时间筛选的最大天数
    private static final int MAX_DAYS = 366;

    @Autowired
    private FriendAppealClient friendAppealClient;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionCaseMapper questionCaseMapper;

    // 分页查询：题目名称先在题目表解析为题目ID，时间换算为起点，再交给 oj-friend 查询
    @Override
    public TableDataResult<AppealVO> list(AppealQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new AppealQueryDTO();
        }
        if (queryDTO.getDays() != null && (queryDTO.getDays() <= 0 || queryDTO.getDays() > MAX_DAYS)) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        FriendAppealQueryDTO friendQuery = BeanUtil.copyProperties(queryDTO, FriendAppealQueryDTO.class);
        friendQuery.setStartTime(queryDTO.getDays() == null ? null : LocalDateTime.now().minusDays(queryDTO.getDays()));
        if (StrUtil.isNotBlank(queryDTO.getTitle())) {
            List<Long> questionIds = questionMapper.selectList(new LambdaQueryWrapper<TbQuestion>()
                            .select(TbQuestion::getQuestionId)
                            .like(TbQuestion::getTitle, queryDTO.getTitle().trim()))
                    .stream()
                    .map(TbQuestion::getQuestionId)
                    .toList();
            if (questionIds.isEmpty()) {
                return TableDataResult.empty();
            }
            friendQuery.setQuestionIds(questionIds);
        }
        FriendPageVO<FriendAppealVO> page = friendAppealClient.listAppeals(friendQuery);
        if (page.getRows().isEmpty()) {
            return TableDataResult.empty();
        }
        List<AppealVO> voList = AppealConverter.toVOList(page.getRows());
        fillNames(voList);
        return TableDataResult.success(voList, page.getTotal());
    }

    // 详情：按题目当前的用例补逐用例输入与预期输出
    @Override
    public AppealDetailVO getDetail(Long appealId) {
        FriendAppealDetailVO detail = friendAppealClient.getAppeal(appealId);
        if (detail == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        AppealDetailVO vo = AppealConverter.toDetailVO(detail, questionCaseMapper.selectJudgeOrdered(detail.getQuestionId()));
        fillNames(List.of(vo));
        return vo;
    }

    // 裁定：只接受存疑、通过、不通过；裁定人取自登录上下文；申诉不存在或已是终态时给出明确提示
    @Override
    public void handle(Long appealId, AppealHandleDTO handleDTO) {
        AppealStatusEnum target = AppealStatusEnum.getByCode(handleDTO.getStatus());
        if (target == null || target == AppealStatusEnum.PENDING) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        FriendAppealHandleDTO request = new FriendAppealHandleDTO();
        request.setStatus(target.getCode());
        request.setHandlerId(SecurityUtils.getUserId());
        if (!friendAppealClient.handleAppeal(appealId, request)) {
            throw new ServiceException(friendAppealClient.getAppeal(appealId) == null
                    ? ResultCode.FAILED_NOT_EXISTS : ResultCode.FAILED_APPEAL_HANDLED);
        }
    }

    // 待修题数量：成立申诉的最近裁定时间晚于该题用例的最后修改时间（修改用例会删旧建新，以用例创建时间为准）
    @Override
    public Map<Long, Integer> countUpheldToFix(List<Long> questionIds) {
        if (CollUtil.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        List<FriendAppealQuestionStatVO> stats = friendAppealClient.upheldStats(questionIds);
        if (stats.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, LocalDateTime> caseChangedAt = questionCaseMapper.selectList(new LambdaQueryWrapper<TbQuestionCase>()
                        .select(TbQuestionCase::getQuestionId, TbQuestionCase::getCreateTime)
                        .in(TbQuestionCase::getQuestionId, stats.stream().map(FriendAppealQuestionStatVO::getQuestionId).toList()))
                .stream()
                .filter(c -> c.getCreateTime() != null)
                .collect(Collectors.toMap(TbQuestionCase::getQuestionId, TbQuestionCase::getCreateTime,
                        (a, b) -> a.isAfter(b) ? a : b));
        Map<Long, Integer> result = new HashMap<>();
        for (FriendAppealQuestionStatVO stat : stats) {
            LocalDateTime changedAt = caseChangedAt.get(stat.getQuestionId());
            if (stat.getLatestHandleTime() != null && (changedAt == null || stat.getLatestHandleTime().isAfter(changedAt))) {
                result.put(stat.getQuestionId(), stat.getUpheldCount());
            }
        }
        return result;
    }

    // 批量补申诉人昵称与题目名称，避免 N+1
    private void fillNames(List<? extends AppealBaseVO> voList) {
        Set<Long> userIds = voList.stream().map(AppealBaseVO::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> questionIds = voList.stream().map(AppealBaseVO::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nickNames = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectList(new LambdaQueryWrapper<TbUser>()
                        .select(TbUser::getUserId, TbUser::getNickName)
                        .in(TbUser::getUserId, userIds))
                .stream()
                .filter(user -> user.getNickName() != null)
                .collect(Collectors.toMap(TbUser::getUserId, TbUser::getNickName));
        Map<Long, String> titles = questionIds.isEmpty() ? Collections.emptyMap()
                : questionMapper.selectList(new LambdaQueryWrapper<TbQuestion>()
                        .select(TbQuestion::getQuestionId, TbQuestion::getTitle)
                        .in(TbQuestion::getQuestionId, questionIds))
                .stream()
                .collect(Collectors.toMap(TbQuestion::getQuestionId, TbQuestion::getTitle));
        for (AppealBaseVO vo : voList) {
            vo.setNickName(nickNames.get(vo.getUserId()));
            vo.setQuestionTitle(titles.get(vo.getQuestionId()));
        }
    }
}
