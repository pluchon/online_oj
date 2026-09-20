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
import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.domain.TbMessageText;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.ExamEnrollDTO;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.mapper.ExamMapper;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// C端竞赛业务实现类
@Slf4j
@Service
public class ExamServiceImpl implements ExamService {

    // 已发布状态常量
    private static final int STATUS_PUBLISHED = 1;

    // 未完赛分类标识
    private static final int TYPE_UNFINISH = 0;

    // 历史竞赛分类标识
    private static final int TYPE_HISTORY = 1;

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
        int targetType = queryDTO.getType() != null && queryDTO.getType() == TYPE_HISTORY ? TYPE_HISTORY : TYPE_UNFINISH;

        // 若无附加搜索条件，走 Redis 缓存快速通道
        boolean hasFilter = StringUtils.hasText(queryDTO.getTitle())
                || StringUtils.hasText(queryDTO.getStartTime())
                || StringUtils.hasText(queryDTO.getEndTime());

        if (!hasFilter) {
            return examCacheManager.getExamList(targetType, queryDTO.getPageNum(), queryDTO.getPageSize());
        }

        // 带有标题或时间区间检索时走数据库查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<TbExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbExam::getStatus, STATUS_PUBLISHED);

        // 标题模糊筛选
        if (StringUtils.hasText(queryDTO.getTitle())) {
            wrapper.like(TbExam::getTitle, queryDTO.getTitle().trim());
        }

        // 时间区间筛选
        if (StringUtils.hasText(queryDTO.getStartTime())) {
            wrapper.ge(TbExam::getStartTime, queryDTO.getStartTime());
        }
        if (StringUtils.hasText(queryDTO.getEndTime())) {
            wrapper.le(TbExam::getEndTime, queryDTO.getEndTime());
        }

        LocalDateTime now = LocalDateTime.now();
        if (targetType == TYPE_UNFINISH) {
            wrapper.gt(TbExam::getEndTime, now);
            wrapper.orderByAsc(TbExam::getStartTime);
        } else {
            wrapper.le(TbExam::getEndTime, now);
            wrapper.orderByDesc(TbExam::getEndTime);
        }

        List<TbExam> examList = examMapper.selectList(wrapper);
        List<ExamVO> voList = ExamConverter.toVOList(examList);
        Long currentUserId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        examCacheManager.populateIsEnter(voList, currentUserId);
        return voList;
    }

    // 分页查询未完赛竞赛列表实现
    @Override
    public List<ExamVO> getUnfinishList(ExamQueryDTO queryDTO) {
        queryDTO.setType(TYPE_UNFINISH);
        return list(queryDTO);
    }

    // 分页查询历史竞赛列表实现
    @Override
    public List<ExamVO> getHistoryList(ExamQueryDTO queryDTO) {
        queryDTO.setType(TYPE_HISTORY);
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
        if (exam == null || exam.getStatus() != STATUS_PUBLISHED) {
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
    public List<UserExamVO> getMyExamList(PageQuery pageQuery) {
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        int pageNum = pageQuery != null && pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1;
        int pageSize = pageQuery != null && pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10;
        return examCacheManager.getMyExamList(userId, pageNum, pageSize);
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

    // 获取当前登录用户在指定竞赛中的成绩与排名
    @Override
    public ExamRankVO getMyExamRank(Long examId) {
        if (examId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        Long currentUserId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (currentUserId == null) {
            return null;
        }

        List<ExamRankVO> fullRankList = getOrCalculateFullRankList(examId);
        if (CollUtil.isEmpty(fullRankList)) {
            return null;
        }

        for (ExamRankVO vo : fullRankList) {
            if (Objects.equals(vo.getUserId(), currentUserId)) {
                ExamRankVO myVO = new ExamRankVO();
                myVO.setExamRank(vo.getExamRank());
                myVO.setUserId(vo.getUserId());
                myVO.setNickName(vo.getNickName());
                myVO.setHeadImage(vo.getHeadImage());
                myVO.setScore(vo.getScore());
                myVO.setAcceptCount(vo.getAcceptCount());
                myVO.setSubmitCount(vo.getSubmitCount());
                myVO.setLastSubmitTime(vo.getLastSubmitTime());
                myVO.setIsCurrentUser(true);
                return myVO;
            }
        }
        return null;
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
            text.setCreateBy(0L);
            text.setCreateTime(LocalDateTime.now());
            messageTextMapper.insert(text);

            TbMessage message = new TbMessage();
            message.setTextId(text.getTextId());
            message.setSendId(0L);
            message.setRecId(vo.getUserId());
            message.setIsRead(0);
            message.setCreateBy(0L);
            message.setCreateTime(LocalDateTime.now());
            messageMapper.insert(message);

            // 同步写入 Redis 缓存与原子自增未读数
            messageCacheManager.saveMessageTextCache(text);
            messageCacheManager.pushUserMessage(vo.getUserId(), text.getTextId());
        }

        log.info("竞赛 {} 结算战报通知发送完成", examId);
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
                    boolean isAc = qSubmits.stream().anyMatch(s -> Integer.valueOf(1).equals(s.getPass()));
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
