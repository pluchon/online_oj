package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import cn.nuonuoya.friend.cache.ExamCacheManager;
import cn.nuonuoya.friend.cache.MessageCacheManager;
import cn.nuonuoya.friend.cache.UserCacheManager;
import cn.nuonuoya.friend.converter.ExamConverter;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbExamQuestion;
import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.domain.TbMessageText;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.ExamEnrollDTO;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.enums.ExamListTypeEnum;
import cn.nuonuoya.friend.enums.ExamPublishStatusEnum;
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
import cn.nuonuoya.security.exception.ServiceException;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    // 竞赛排名列表缓存键前缀
    private static final String EXAM_RANK_LIST_PREFIX = "exam:rank:";

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
                wrapper.orderByAsc(TbExam::getStartTime);
            } else {
                wrapper.le(TbExam::getEndTime, now);
                wrapper.orderByDesc(TbExam::getEndTime);
            }
        } else {
            // 全部竞赛按开赛时间倒序
            wrapper.orderByDesc(TbExam::getStartTime);
        }

        List<TbExam> examList = examMapper.selectList(wrapper);
        List<ExamVO> voList = ExamConverter.toVOList(examList);
        Long currentUserId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        examCacheManager.populateIsEnter(voList, currentUserId);
        populateExamCountFields(voList);
        return voList;
    }

    // 分页查询未完赛竞赛列表实现
    @Override
    public List<ExamVO> getUnfinishList(ExamQueryDTO queryDTO) {
        queryDTO.setType(ExamListTypeEnum.UNFINISHED.getCode());
        return list(queryDTO);
    }

    // 分页查询历史竞赛列表实现
    @Override
    public List<ExamVO> getHistoryList(ExamQueryDTO queryDTO) {
        queryDTO.setType(ExamListTypeEnum.HISTORY.getCode());
        return list(queryDTO);
    }

    // 报名参加竞赛
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enroll(ExamEnrollDTO enrollDTO) {
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        Long examId = enrollDTO.getExamId();
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
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
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
                wrapper.orderByAsc(TbExam::getStartTime);
            } else {
                wrapper.le(TbExam::getEndTime, now);
                wrapper.orderByDesc(TbExam::getEndTime);
            }
        } else {
            wrapper.orderByDesc(TbExam::getStartTime);
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
        if (exam == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        ExamVO vo = ExamConverter.toVO(exam);
        Long currentUserId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
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

        Long currentUserId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        for (ExamRankVO vo : pageList) {
            vo.setIsCurrentUser(currentUserId != null && Objects.equals(vo.getUserId(), currentUserId));
        }

        return TableDataResult.success(pageList, total);
    }

    // 结算指定竞赛排名并发送战报通知
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleExamRank(Long examId) {
        if (examId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        TbExam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        // 竞赛结束前不允许结算
        if (exam.getEndTime() == null || LocalDateTime.now().isBefore(exam.getEndTime())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_RANK_NOT_PUBLISHED);
        }

        // 重新计算最新完整排名列表并同步数据库
        List<ExamRankVO> fullRankList = calculateAndPersistRanks(exam);
        if (CollUtil.isEmpty(fullRankList)) {
            log.info("竞赛 {} 无参赛选手，无需发送战报通知", examId);
            return;
        }

        int totalParticipants = fullRankList.size();
        log.info("开始为竞赛 {} 发送结算排名通知，共计 {} 名选手", examId, totalParticipants);

        // 为每位参赛选手生成定制战报消息
        for (ExamRankVO vo : fullRankList) {
            TbMessageText text = new TbMessageText();
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

            // 同步写入 Redis 缓存与原子自增未读数
            messageCacheManager.saveMessageTextCache(text);
            messageCacheManager.pushUserMessage(vo.getUserId(), text.getTextId());
        }

        log.info("竞赛 {} 结算战报通知发送完成", examId);
    }

    // 批量装配竞赛列表的参赛人数与题目数量，避免 N+1
    private void populateExamCountFields(List<ExamVO> voList) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        List<Long> examIds = voList.stream().map(ExamVO::getExamId).collect(Collectors.toList());

        // 批量统计参赛人数：按 examId 分组 count tb_user_exam
        List<TbUserExam> userExamRecords = userExamMapper.selectList(
                new LambdaQueryWrapper<TbUserExam>()
                        .select(TbUserExam::getExamId)
                        .in(TbUserExam::getExamId, examIds));
        Map<Long, Integer> enterCountMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userExamRecords)) {
            userExamRecords.stream()
                    .collect(Collectors.groupingBy(TbUserExam::getExamId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)))
                    .forEach(enterCountMap::put);
        }

        // 批量统计题目数量：按 examId 分组 count tb_exam_question
        List<TbExamQuestion> examQuestions = examQuestionMapper.selectList(
                new LambdaQueryWrapper<TbExamQuestion>()
                        .select(TbExamQuestion::getExamId)
                        .in(TbExamQuestion::getExamId, examIds));
        Map<Long, Integer> questionCountMap = new HashMap<>();
        if (CollUtil.isNotEmpty(examQuestions)) {
            examQuestions.stream()
                    .collect(Collectors.groupingBy(TbExamQuestion::getExamId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)))
                    .forEach(questionCountMap::put);
        }

        // 将统计结果装配到 VO
        for (ExamVO vo : voList) {
            vo.setEnterCount(enterCountMap.getOrDefault(vo.getExamId(), 0));
            vo.setQuestionCount(questionCountMap.getOrDefault(vo.getExamId(), 0));
        }
    }

    // 批量装配我的竞赛列表的参赛人数与题目数量，避免 N+1
    private void populateUserExamCountFields(List<UserExamVO> voList) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        List<Long> examIds = voList.stream().map(UserExamVO::getExamId).collect(Collectors.toList());

        // 批量统计参赛人数
        List<TbUserExam> userExamRecords = userExamMapper.selectList(
                new LambdaQueryWrapper<TbUserExam>()
                        .select(TbUserExam::getExamId)
                        .in(TbUserExam::getExamId, examIds));
        Map<Long, Integer> enterCountMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userExamRecords)) {
            userExamRecords.stream()
                    .collect(Collectors.groupingBy(TbUserExam::getExamId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)))
                    .forEach(enterCountMap::put);
        }

        // 批量统计题目数量
        List<TbExamQuestion> examQuestions = examQuestionMapper.selectList(
                new LambdaQueryWrapper<TbExamQuestion>()
                        .select(TbExamQuestion::getExamId)
                        .in(TbExamQuestion::getExamId, examIds));
        Map<Long, Integer> questionCountMap = new HashMap<>();
        if (CollUtil.isNotEmpty(examQuestions)) {
            examQuestions.stream()
                    .collect(Collectors.groupingBy(TbExamQuestion::getExamId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)))
                    .forEach(questionCountMap::put);
        }

        for (UserExamVO vo : voList) {
            vo.setEnterCount(enterCountMap.getOrDefault(vo.getExamId(), 0));
            vo.setQuestionCount(questionCountMap.getOrDefault(vo.getExamId(), 0));
        }
    }

    // 获取或实时计算完整排名列表（优先走 Redis 缓存）
    private List<ExamRankVO> getOrCalculateFullRankList(Long examId) {
        String rankCacheKey = EXAM_RANK_LIST_PREFIX + examId;
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

        return calculateAndPersistRanks(exam);
    }

    // 核心排名计算、排序、持久化与缓存方法
    private List<ExamRankVO> calculateAndPersistRanks(TbExam exam) {
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

        // 若竞赛已完赛，落库同步更新 tb_user_exam 中的得分与最终排名
        LocalDateTime now = LocalDateTime.now();
        boolean isFinished = exam.getEndTime() != null && exam.getEndTime().isBefore(now);

        if (isFinished) {
            Map<Long, ExamRankVO> rankMap = rankList.stream().collect(Collectors.toMap(ExamRankVO::getUserId, r -> r));
            for (TbUserExam ue : userExams) {
                ExamRankVO rankVO = rankMap.get(ue.getUserId());
                if (rankVO != null) {
                    boolean needUpdate = !Objects.equals(ue.getScore(), rankVO.getScore())
                            || !Objects.equals(ue.getExamRank(), rankVO.getExamRank());
                    if (needUpdate) {
                        TbUserExam update = new TbUserExam();
                        update.setUserExamId(ue.getUserExamId());
                        update.setScore(rankVO.getScore());
                        update.setExamRank(rankVO.getExamRank());
                        update.setUpdateTime(now);
                        userExamMapper.updateById(update);
                    }
                }
            }
        }

        // 写入 Redis 缓存（已完赛保留 24 小时，进行中保留 3 分钟）
        String rankCacheKey = EXAM_RANK_LIST_PREFIX + examId;
        redisService.setCacheObject(rankCacheKey, JSON.toJSONString(rankList), isFinished ? 24L : 3L, isFinished ? TimeUnit.HOURS : TimeUnit.MINUTES);

        return rankList;
    }
}
