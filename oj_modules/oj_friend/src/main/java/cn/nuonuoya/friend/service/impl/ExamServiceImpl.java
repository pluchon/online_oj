package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.cache.ExamCacheManager;
import cn.nuonuoya.friend.cache.MessageCacheManager;
import cn.nuonuoya.friend.cache.QuestionCacheManager;
import cn.nuonuoya.friend.cache.UserCacheManager;
import cn.nuonuoya.friend.constants.FriendCacheConstants;
import cn.nuonuoya.friend.converter.ExamConverter;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbExamQuestion;
import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.domain.TbMessageText;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.enums.ExamListTypeEnum;
import cn.nuonuoya.friend.enums.ExamPublishStatusEnum;
import cn.nuonuoya.friend.enums.ExamRankSettledEnum;
import cn.nuonuoya.friend.enums.MessageTypeEnum;
import cn.nuonuoya.friend.enums.MessageReadStatusEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.ExamQuestionMapper;
import cn.nuonuoya.friend.mapper.MessageMapper;
import cn.nuonuoya.friend.mapper.MessageTextMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.ExamService;
import cn.nuonuoya.friend.vo.ExamRankVO;
import cn.nuonuoya.friend.vo.ExamVO;
import cn.nuonuoya.friend.vo.UserExamVO;
import cn.nuonuoya.friend.vo.UserVO;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.security.utils.SecurityUtils;
import cn.nuonuoya.security.exception.ServiceException;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.nuonuoya.mybatis.utils.TransactionUtils;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// C端竞赛业务实现类
@Slf4j
@Service
public class ExamServiceImpl implements ExamService {

    // 系统消息发送方标识
    private static final Long SYSTEM_SENDER_ID = 0L;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    @Autowired
    private ExamCacheManager examCacheManager;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private UserCacheManager userCacheManager;

    @Autowired
    private RedisService redisService;

    @Autowired
    private MessageTextMapper messageTextMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageCacheManager messageCacheManager;

    @Autowired
    private QuestionCacheManager questionCacheManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // 分页查询竞赛列表实现
    @Override
    public List<ExamVO> list(ExamQueryDTO queryDTO) {
        Integer queryType = queryDTO.getType();
        boolean hasExtraFilter = StringUtils.hasText(queryDTO.getTitle())
                || StringUtils.hasText(queryDTO.getStartTime())
                || StringUtils.hasText(queryDTO.getEndTime());

        // type 有值且无附加过滤条件时，走 Redis 缓存快速通道
        if (queryType != null && !hasExtraFilter) {
            int targetType = ExamListTypeEnum.HISTORY.getCode().equals(queryType)
                    ? ExamListTypeEnum.HISTORY.getCode()
                    : ExamListTypeEnum.UNFINISHED.getCode();
            List<ExamVO> voList = examCacheManager.getExamList(targetType, queryDTO.getPageNum(), queryDTO.getPageSize());
            populateExamCountFields(voList);
            return voList;
        }

        // type=null（全部竞赛）或带附加过滤条件时走数据库查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<TbExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode());

        // 标题模糊筛选
        if (StringUtils.hasText(queryDTO.getTitle())) {
            wrapper.like(TbExam::getTitle, queryDTO.getTitle().trim());
        }

        // 时间区间按开赛时间过滤（前端约定：start_time >= startTime AND start_time <= endTime）
        if (StringUtils.hasText(queryDTO.getStartTime())) {
            wrapper.ge(TbExam::getStartTime, queryDTO.getStartTime());
        }
        if (StringUtils.hasText(queryDTO.getEndTime())) {
            wrapper.le(TbExam::getStartTime, queryDTO.getEndTime());
        }

        // 完赛状态过滤：null=全部，0=未完赛，1=历史竞赛
        LocalDateTime now = LocalDateTime.now();
        if (queryType != null) {
            if (ExamListTypeEnum.UNFINISHED.getCode().equals(queryType)) {
                wrapper.gt(TbExam::getEndTime, now);
                wrapper.orderByAsc(TbExam::getStartTime, TbExam::getExamId);
            } else {
                wrapper.le(TbExam::getEndTime, now);
                wrapper.orderByDesc(TbExam::getEndTime, TbExam::getExamId);
            }
        } else {
            // 全部竞赛按开赛时间倒序
            wrapper.orderByDesc(TbExam::getStartTime, TbExam::getExamId);
        }

        List<TbExam> examList = examMapper.selectList(wrapper);
        List<ExamVO> voList = ExamConverter.toVOList(examList);
        Long currentUserId = SecurityUtils.getUserId();
        examCacheManager.populateIsEnter(voList, currentUserId);
        populateExamCountFields(voList);
        return voList;
    }

    // 报名参加竞赛
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enroll(Long examId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        TbExam exam = examMapper.selectById(examId);
        if (exam == null || !ExamPublishStatusEnum.PUBLISHED.getCode().equals(exam.getStatus())) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }

        // 边界校验：已开赛或已结束的竞赛不可报名
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isAfter(exam.getStartTime())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_STARTED_OR_FINISHED);
        }

        // 边界校验：防重复报名
        Long count = userExamMapper.selectCount(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getUserId, userId)
                .eq(TbUserExam::getExamId, examId));
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_USER_EXAM_EXISTS);
        }

        // 插入关联记录
        TbUserExam userExam = new TbUserExam();
        userExam.setUserId(userId);
        userExam.setExamId(examId);
        userExamMapper.insert(userExam);

        // 同步写入Redis已报名列表缓存
        examCacheManager.addUserExamCache(userId, examId);
    }

    // 分页查询当前用户已报名的竞赛列表
    @Override
    public List<UserExamVO> getMyExamList(ExamQueryDTO queryDTO) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        int pageNum = queryDTO != null && queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1;
        int pageSize = queryDTO != null ? queryDTO.getPageSize() : 8;

        // 无过滤条件时走 Redis 缓存快速通道
        boolean hasFilter = queryDTO != null && (
                queryDTO.getType() != null
                || StringUtils.hasText(queryDTO.getTitle())
                || StringUtils.hasText(queryDTO.getStartTime())
                || StringUtils.hasText(queryDTO.getEndTime()));
        if (!hasFilter) {
            List<UserExamVO> cachedList = examCacheManager.getMyExamList(userId, pageNum, pageSize);
            populateUserExamCountFields(cachedList);
            return cachedList;
        }

        // 带过滤条件时走 DB 查询
        // 1. 获取该用户报名的所有竞赛ID
        List<TbUserExam> allUserExams = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                .select(TbUserExam::getExamId)
                .eq(TbUserExam::getUserId, userId));
        if (CollUtil.isEmpty(allUserExams)) {
            return Collections.emptyList();
        }
        List<Long> enrolledExamIds = allUserExams.stream().map(TbUserExam::getExamId).collect(Collectors.toList());

        // 2. 带过滤条件分页查询竞赛信息
        PageHelper.startPage(pageNum, pageSize);
        LambdaQueryWrapper<TbExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode());
        wrapper.in(TbExam::getExamId, enrolledExamIds);

        if (StringUtils.hasText(queryDTO.getTitle())) {
            wrapper.like(TbExam::getTitle, queryDTO.getTitle().trim());
        }
        // 时间区间按开赛时间过滤
        if (StringUtils.hasText(queryDTO.getStartTime())) {
            wrapper.ge(TbExam::getStartTime, queryDTO.getStartTime());
        }
        if (StringUtils.hasText(queryDTO.getEndTime())) {
            wrapper.le(TbExam::getStartTime, queryDTO.getEndTime());
        }

        // 完赛状态过滤
        LocalDateTime now = LocalDateTime.now();
        if (queryDTO.getType() != null) {
            if (ExamListTypeEnum.UNFINISHED.getCode().equals(queryDTO.getType())) {
                wrapper.gt(TbExam::getEndTime, now);
                wrapper.orderByAsc(TbExam::getStartTime, TbExam::getExamId);
            } else {
                wrapper.le(TbExam::getEndTime, now);
                wrapper.orderByDesc(TbExam::getEndTime, TbExam::getExamId);
            }
        } else {
            wrapper.orderByDesc(TbExam::getStartTime, TbExam::getExamId);
        }

        List<TbExam> examList = examMapper.selectList(wrapper);
        if (CollUtil.isEmpty(examList)) {
            return Collections.emptyList();
        }

        // 3. 批量获取用户竞赛得分与排名记录
        List<Long> pageExamIds = examList.stream().map(TbExam::getExamId).collect(Collectors.toList());
        List<TbUserExam> records = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getUserId, userId)
                .in(TbUserExam::getExamId, pageExamIds));
        Map<Long, TbUserExam> recordMap = CollUtil.isNotEmpty(records)
                ? records.stream().collect(Collectors.toMap(TbUserExam::getExamId, u -> u, (k1, k2) -> k1))
                : Collections.emptyMap();

        List<UserExamVO> voList = new ArrayList<>();
        for (TbExam exam : examList) {
            TbUserExam ue = recordMap.get(exam.getExamId());
            voList.add(ExamConverter.toUserExamVO(exam, ue));
        }
        populateUserExamCountFields(voList);
        return voList;
    }

    // 获取指定竞赛详情
    @Override
    public ExamVO getExamDetail(Long examId) {
        if (examId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        TbExam exam = examMapper.selectById(examId);
        if (exam == null || !ExamPublishStatusEnum.PUBLISHED.getCode().equals(exam.getStatus())) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        ExamVO vo = ExamConverter.toVO(exam);
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null) {
            Long count = userExamMapper.selectCount(new LambdaQueryWrapper<TbUserExam>()
                    .eq(TbUserExam::getUserId, currentUserId)
                    .eq(TbUserExam::getExamId, examId));
            vo.setIsEnter(count != null && count > 0);
        } else {
            vo.setIsEnter(false);
        }
        return vo;
    }

    // 分页查询竞赛选手得分与排名榜单
    @Override
    public TableDataResult<ExamRankVO> getExamRankList(Long examId, PageQuery pageQuery) {
        if (examId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 排名在竞赛结束后才公布
        TbExam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        if (exam.getEndTime() == null || LocalDateTime.now().isBefore(exam.getEndTime())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_RANK_NOT_PUBLISHED);
        }

        List<ExamRankVO> fullRankList = getOrCalculateFullRankList(examId);
        if (CollUtil.isEmpty(fullRankList)) {
            return TableDataResult.empty();
        }

        int total = fullRankList.size();
        int pageNum = pageQuery != null && pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1;
        int pageSize = pageQuery != null && pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 20;

        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<ExamRankVO> pageList;
        if (start >= total) {
            pageList = Collections.emptyList();
        } else {
            pageList = new ArrayList<>(fullRankList.subList(start, end));
        }

        Long currentUserId = SecurityUtils.getUserId();
        for (ExamRankVO vo : pageList) {
            vo.setIsCurrentUser(currentUserId != null && Objects.equals(vo.getUserId(), currentUserId));
        }

        return TableDataResult.success(pageList, total);
    }

    // 刷新竞赛缓存（供管理端变更竞赛与定时任务调用）
    @Override
    public int refreshExamCache(Long examId) {
        if (examId != null) {
            examCacheManager.evictExamDetail(examId);
            questionCacheManager.evictListCache(examId);
        }
        return examCacheManager.rebuildListCaches();
    }

    // 结算所有已结束且未结算的竞赛（每场独立事务，单场失败不影响其他场次），返回成功结算场数
    @Override
    public int settleFinishedExams() {
        List<TbExam> exams = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                .eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode())
                .eq(TbExam::getRankSettled, ExamRankSettledEnum.UNSETTLED.getCode())
                .le(TbExam::getEndTime, LocalDateTime.now()));
        int settled = 0;
        for (TbExam exam : exams) {
            try {
                if (Boolean.TRUE.equals(transactionTemplate.execute(status -> settleExam(exam)))) {
                    settled++;
                }
            } catch (Exception e) {
                log.error("竞赛结算失败, examId = {}", exam.getExamId(), e);
            }
        }
        return settled;
    }

    // 结算单场竞赛：先抢占结算标记保证只执行一次，再落库排名并发送战报
    private boolean settleExam(TbExam exam) {
        int claimed = examMapper.update(null, new LambdaUpdateWrapper<TbExam>()
                .set(TbExam::getRankSettled, ExamRankSettledEnum.SETTLED.getCode())
                .eq(TbExam::getExamId, exam.getExamId())
                .eq(TbExam::getRankSettled, ExamRankSettledEnum.UNSETTLED.getCode()));
        if (claimed == 0) {
            return false;
        }
        List<ExamRankVO> rankList = calculateRanks(exam);
        if (CollUtil.isNotEmpty(rankList)) {
            persistRanks(exam.getExamId(), rankList);
            sendRankNotices(exam, rankList);
        }
        log.info("竞赛结算完成, examId = {}, 参赛人数 = {}", exam.getExamId(), rankList.size());
        return true;
    }

    // 批量装配竞赛列表的参赛人数与题目数量，避免 N+1
    private void populateExamCountFields(List<ExamVO> voList) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        ExamCounts counts = countByExamIds(voList.stream().map(ExamVO::getExamId).collect(Collectors.toList()));
        for (ExamVO vo : voList) {
            vo.setEnterCount(counts.enterCount(vo.getExamId()));
            vo.setQuestionCount(counts.questionCount(vo.getExamId()));
        }
    }

    // 批量装配我的竞赛列表的参赛人数与题目数量，避免 N+1
    private void populateUserExamCountFields(List<UserExamVO> voList) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        ExamCounts counts = countByExamIds(voList.stream().map(UserExamVO::getExamId).collect(Collectors.toList()));
        for (UserExamVO vo : voList) {
            vo.setEnterCount(counts.enterCount(vo.getExamId()));
            vo.setQuestionCount(counts.questionCount(vo.getExamId()));
        }
    }

    // 按竞赛ID批量统计参赛人数与题目数量
    private ExamCounts countByExamIds(List<Long> examIds) {
        Map<Long, Long> enterCountMap = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                        .select(TbUserExam::getExamId)
                        .in(TbUserExam::getExamId, examIds))
                .stream()
                .collect(Collectors.groupingBy(TbUserExam::getExamId, Collectors.counting()));
        Map<Long, Long> questionCountMap = examQuestionMapper.selectList(new LambdaQueryWrapper<TbExamQuestion>()
                        .select(TbExamQuestion::getExamId)
                        .in(TbExamQuestion::getExamId, examIds))
                .stream()
                .collect(Collectors.groupingBy(TbExamQuestion::getExamId, Collectors.counting()));
        return new ExamCounts(enterCountMap, questionCountMap);
    }

    // 竞赛参赛人数与题目数量统计结果
    private record ExamCounts(Map<Long, Long> enterCountMap, Map<Long, Long> questionCountMap) {

        // 指定竞赛的参赛人数
        int enterCount(Long examId) {
            return enterCountMap.getOrDefault(examId, 0L).intValue();
        }

        // 指定竞赛的题目数量
        int questionCount(Long examId) {
            return questionCountMap.getOrDefault(examId, 0L).intValue();
        }
    }

    // 获取或实时计算完整排名列表（优先走 Redis 缓存）
    private List<ExamRankVO> getOrCalculateFullRankList(Long examId) {
        String rankCacheKey = FriendCacheConstants.EXAM_RANK_LIST_KEY + examId;
        String json = redisService.getCacheObject(rankCacheKey, String.class);
        if (StringUtils.hasText(json)) {
            List<ExamRankVO> cachedList = JSON.parseArray(json, ExamRankVO.class);
            if (CollUtil.isNotEmpty(cachedList)) {
                return cachedList;
            }
        }

        TbExam exam = examMapper.selectById(examId);
        if (exam == null) {
            return Collections.emptyList();
        }

        return calculateRanks(exam);
    }

    // 计算完整排名（总分、AC 数、最后提交时间、用户ID 依次排序）并写入缓存，不落库
    private List<ExamRankVO> calculateRanks(TbExam exam) {
        Long examId = exam.getExamId();
        List<TbUserExam> userExams = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getExamId, examId));
        if (CollUtil.isEmpty(userExams)) {
            return Collections.emptyList();
        }

        // 查询本次竞赛中的所有提交记录
        List<TbUserSubmit> allSubmits = userSubmitMapper.selectList(new LambdaQueryWrapper<TbUserSubmit>()
                .eq(TbUserSubmit::getExamId, examId));
        Map<Long, List<TbUserSubmit>> userSubmitMap = CollUtil.isNotEmpty(allSubmits)
                ? allSubmits.stream().collect(Collectors.groupingBy(TbUserSubmit::getUserId))
                : Collections.emptyMap();

        List<ExamRankVO> rankList = new ArrayList<>(userExams.size());

        for (TbUserExam ue : userExams) {
            Long userId = ue.getUserId();
            List<TbUserSubmit> submits = userSubmitMap.getOrDefault(userId, Collections.emptyList());

            int totalScore = 0;
            int acCount = 0;
            int submitCount = submits.size();
            LocalDateTime lastSubmitTime = null;

            if (CollUtil.isNotEmpty(submits)) {
                // 按题目聚合计算每题最高分与通过状态
                Map<Long, List<TbUserSubmit>> qMap = submits.stream().collect(Collectors.groupingBy(TbUserSubmit::getQuestionId));
                for (List<TbUserSubmit> qSubmits : qMap.values()) {
                    int maxScore = qSubmits.stream().mapToInt(s -> s.getScore() != null ? s.getScore() : 0).max().orElse(0);
                    totalScore += maxScore;
                    boolean isAc = qSubmits.stream().anyMatch(s -> SubmitPassEnum.PASS.getCode().equals(s.getPass()));
                    if (isAc) {
                        acCount++;
                    }
                }
                lastSubmitTime = submits.stream()
                        .map(TbUserSubmit::getCreateTime)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(ue.getCreateTime());
            } else {
                totalScore = ue.getScore() != null ? ue.getScore() : 0;
                lastSubmitTime = ue.getCreateTime();
            }

            ExamRankVO vo = new ExamRankVO();
            vo.setUserId(userId);
            vo.setScore(totalScore);
            vo.setAcceptCount(acCount);
            vo.setSubmitCount(submitCount);
            vo.setLastSubmitTime(lastSubmitTime);
            rankList.add(vo);
        }

        // 统一排名排序规则：1.总分降序 2.AC题数降序 3.最后提交时间升序 4.用户ID升序
        rankList.sort((a, b) -> {
            int scoreCmp = Integer.compare(b.getScore(), a.getScore());
            if (scoreCmp != 0) return scoreCmp;

            int acCmp = Integer.compare(b.getAcceptCount(), a.getAcceptCount());
            if (acCmp != 0) return acCmp;

            if (a.getLastSubmitTime() != null && b.getLastSubmitTime() != null) {
                int timeCmp = a.getLastSubmitTime().compareTo(b.getLastSubmitTime());
                if (timeCmp != 0) return timeCmp;
            }
            return Long.compare(a.getUserId(), b.getUserId());
        });

        // 赋值排名名次与选手个人信息
        for (int i = 0; i < rankList.size(); i++) {
            ExamRankVO vo = rankList.get(i);
            vo.setExamRank(i + 1);

            UserVO user = userCacheManager.getUserById(vo.getUserId());
            if (user != null) {
                vo.setNickName(StringUtils.hasText(user.getNickName()) ? user.getNickName() : "用户_" + vo.getUserId());
                vo.setHeadImage(user.getHeadImage());
            } else {
                vo.setNickName("用户_" + vo.getUserId());
            }
        }

        // 写入 Redis 缓存（已完赛保留较久，进行中短暂缓存）
        boolean isFinished = exam.getEndTime() != null && exam.getEndTime().isBefore(LocalDateTime.now());
        String rankCacheKey = FriendCacheConstants.EXAM_RANK_LIST_KEY + examId;
        if (isFinished) {
            redisService.setCacheObject(rankCacheKey, JSON.toJSONString(rankList), FriendCacheConstants.EXAM_RANK_FINISHED_TTL_HOURS, TimeUnit.HOURS);
        } else {
            redisService.setCacheObject(rankCacheKey, JSON.toJSONString(rankList), FriendCacheConstants.EXAM_RANK_ONGOING_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return rankList;
    }

    // 将最终得分与名次写入报名记录（仅结算时调用）
    private void persistRanks(Long examId, List<ExamRankVO> rankList) {
        Map<Long, ExamRankVO> rankMap = rankList.stream().collect(Collectors.toMap(ExamRankVO::getUserId, r -> r));
        List<TbUserExam> userExams = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getExamId, examId));
        for (TbUserExam ue : userExams) {
            ExamRankVO rankVO = rankMap.get(ue.getUserId());
            if (rankVO == null) {
                continue;
            }
            if (!Objects.equals(ue.getScore(), rankVO.getScore()) || !Objects.equals(ue.getExamRank(), rankVO.getExamRank())) {
                TbUserExam update = new TbUserExam();
                update.setUserExamId(ue.getUserExamId());
                update.setScore(rankVO.getScore());
                update.setExamRank(rankVO.getExamRank());
                userExamMapper.updateById(update);
            }
        }
    }

    // 为每位参赛选手写入战报消息，缓存在事务提交后更新
    private void sendRankNotices(TbExam exam, List<ExamRankVO> rankList) {
        int totalParticipants = rankList.size();
        for (ExamRankVO vo : rankList) {
            TbMessageText text = new TbMessageText();
            text.setMessageType(MessageTypeEnum.EXAM.getCode());
            text.setMessageTitle("竞赛结果通知");
            text.setMessageContent("您参与的竞赛：" + exam.getTitle() + "：本次共参赛" + totalParticipants + "人，您排名：第" + vo.getExamRank() + "名！");
            text.setCreateBy(SYSTEM_SENDER_ID);
            text.setCreateTime(LocalDateTime.now());
            messageTextMapper.insert(text);

            TbMessage message = new TbMessage();
            message.setTextId(text.getTextId());
            message.setSendId(SYSTEM_SENDER_ID);
            message.setRecId(vo.getUserId());
            message.setIsRead(MessageReadStatusEnum.UNREAD.getCode());
            message.setCreateBy(SYSTEM_SENDER_ID);
            message.setCreateTime(LocalDateTime.now());
            messageMapper.insert(message);
        }
        TransactionUtils.afterCommit(() -> rankList.forEach(vo -> messageCacheManager.incrementUnreadCount(vo.getUserId())));
    }

    // 用户已报名且正在进行的竞赛中是否包含该题（不依赖前端是否携带竞赛ID）
    @Override
    public boolean isQuestionInOngoingExam(Long userId, Long questionId) {
        Set<Long> enrolledExamIds = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                        .select(TbUserExam::getExamId)
                        .eq(TbUserExam::getUserId, userId))
                .stream().map(TbUserExam::getExamId).collect(Collectors.toSet());
        if (enrolledExamIds.isEmpty()) {
            return false;
        }
        List<Long> examIds = examQuestionMapper.selectList(new LambdaQueryWrapper<TbExamQuestion>()
                        .select(TbExamQuestion::getExamId)
                        .eq(TbExamQuestion::getQuestionId, questionId)
                        .in(TbExamQuestion::getExamId, enrolledExamIds))
                .stream().map(TbExamQuestion::getExamId).toList();
        if (CollUtil.isEmpty(examIds)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return examMapper.selectCount(new LambdaQueryWrapper<TbExam>()
                .in(TbExam::getExamId, examIds)
                .eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode())
                .le(TbExam::getStartTime, now)
                .ge(TbExam::getEndTime, now)) > 0;
    }
}
