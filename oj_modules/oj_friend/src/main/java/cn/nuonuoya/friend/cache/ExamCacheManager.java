package cn.nuonuoya.friend.cache;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.friend.constants.FriendCacheConstants;
import cn.nuonuoya.friend.converter.ExamConverter;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.enums.ExamListTypeEnum;
import cn.nuonuoya.friend.enums.ExamPublishStatusEnum;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.vo.ExamVO;
import cn.nuonuoya.friend.vo.UserExamVO;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.security.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// C端竞赛缓存管理组件（竞赛ID列表 + 竞赛详情，分页结果以 PageHelper Page 返回以保留总数）
@Component
public class ExamCacheManager {

    @Autowired
    private RedisService redisService;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    // 从缓存分页获取未完赛或历史竞赛列表
    public List<ExamVO> getExamList(int type, int pageNum, int pageSize) {
        String listKey = getListKey(type);
        if (Boolean.FALSE.equals(redisService.hasKey(listKey))) {
            initCache(type);
        }
        long total = getListTotal(listKey);
        List<Long> idList = getPageIds(listKey, total, pageNum, pageSize);
        if (CollUtil.isEmpty(idList)) {
            return emptyPage(pageNum, pageSize, total);
        }

        List<TbExam> examList = new ArrayList<>(loadExamDetails(idList).values());
        List<ExamVO> voList = ExamConverter.toVOList(examList);
        populateIsEnter(voList, SecurityUtils.getUserId());
        return toPage(voList, pageNum, pageSize, total);
    }

    // 填充列表中当前用户的报名状态
    public void populateIsEnter(List<ExamVO> voList, Long userId) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        Set<Long> enrolledExamIds = getUserEnrolledExamIds(userId);
        for (ExamVO vo : voList) {
            vo.setIsEnter(enrolledExamIds.contains(vo.getExamId()));
        }
    }

    // 获取用户已报名的竞赛ID集合
    public Set<Long> getUserEnrolledExamIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        String userExamListKey = FriendCacheConstants.USER_EXAM_LIST_KEY + userId;
        if (Boolean.FALSE.equals(redisService.hasKey(userExamListKey))) {
            initUserExamCache(userId);
        }
        List<Long> idList = redisService.getCacheListByRange(userExamListKey, 0, -1, Long.class);
        return CollUtil.isNotEmpty(idList) ? new HashSet<>(idList) : Collections.emptySet();
    }

    // 用户报名成功后向缓存追加记录
    public void addUserExamCache(Long userId, Long examId) {
        if (userId == null || examId == null) {
            return;
        }
        String userExamListKey = FriendCacheConstants.USER_EXAM_LIST_KEY + userId;
        if (Boolean.TRUE.equals(redisService.hasKey(userExamListKey))) {
            redisService.leftPushForList(userExamListKey, examId);
        } else {
            initUserExamCache(userId);
        }
    }

    // 从数据库重建指定用户的已报名竞赛缓存（按报名时间倒序）
    public synchronized void initUserExamCache(Long userId) {
        if (userId == null) {
            return;
        }
        String userExamListKey = FriendCacheConstants.USER_EXAM_LIST_KEY + userId;
        List<TbUserExam> list = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                .select(TbUserExam::getExamId)
                .eq(TbUserExam::getUserId, userId)
                .orderByDesc(TbUserExam::getCreateTime, TbUserExam::getExamId));
        redisService.deleteObject(userExamListKey);
        if (CollUtil.isNotEmpty(list)) {
            List<Long> idList = list.stream().map(TbUserExam::getExamId).collect(Collectors.toList());
            redisService.rightPushAll(userExamListKey, idList);
        }
    }

    // 从缓存分页获取"我的竞赛"列表
    public List<UserExamVO> getMyExamList(Long userId, int pageNum, int pageSize) {
        if (userId == null) {
            return emptyPage(pageNum, pageSize, 0);
        }
        String listKey = FriendCacheConstants.USER_EXAM_LIST_KEY + userId;
        if (Boolean.FALSE.equals(redisService.hasKey(listKey))) {
            initUserExamCache(userId);
        }
        long total = getListTotal(listKey);
        List<Long> examIdList = getPageIds(listKey, total, pageNum, pageSize);
        if (CollUtil.isEmpty(examIdList)) {
            return emptyPage(pageNum, pageSize, total);
        }

        Map<Long, TbExam> examMap = loadExamDetails(examIdList);
        List<TbUserExam> userExamRecords = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getUserId, userId)
                .in(TbUserExam::getExamId, examIdList));
        Map<Long, TbUserExam> recordMap = userExamRecords.stream()
                .collect(Collectors.toMap(TbUserExam::getExamId, u -> u, (k1, k2) -> k1));

        List<UserExamVO> voList = new ArrayList<>(examMap.size());
        for (TbExam exam : examMap.values()) {
            voList.add(ExamConverter.toUserExamVO(exam, recordMap.get(exam.getExamId())));
        }
        return toPage(voList, pageNum, pageSize, total);
    }

    // 从数据库重建指定类型的竞赛列表缓存
    public synchronized void initCache(int type) {
        String listKey = getListKey(type);
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<TbExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode());
        if (type == ExamListTypeEnum.UNFINISHED.getCode()) {
            wrapper.gt(TbExam::getEndTime, now);
            wrapper.orderByAsc(TbExam::getStartTime, TbExam::getExamId);
        } else {
            wrapper.le(TbExam::getEndTime, now);
            wrapper.orderByDesc(TbExam::getEndTime, TbExam::getExamId);
        }

        List<TbExam> list = examMapper.selectList(wrapper);
        redisService.deleteObject(listKey);
        if (CollUtil.isNotEmpty(list)) {
            List<Long> idList = list.stream().map(TbExam::getExamId).collect(Collectors.toList());
            redisService.rightPushAll(listKey, idList);
            for (TbExam exam : list) {
                redisService.setCacheObject(CacheConstants.EXAM_DETAIL_KEY + exam.getExamId(), exam);
            }
        }
    }

    // 根据分类获取对应的列表缓存键
    private String getListKey(int type) {
        return type == ExamListTypeEnum.HISTORY.getCode() ? CacheConstants.EXAM_HISTORY_LIST_KEY : CacheConstants.EXAM_UNFINISH_LIST_KEY;
    }

    // 获取列表缓存总数
    private long getListTotal(String listKey) {
        Long total = redisService.getListSize(listKey);
        return total == null ? 0 : total;
    }

    // 获取列表缓存中指定页的ID，页码越界或列表为空时返回空列表
    private List<Long> getPageIds(String listKey, long total, int pageNum, int pageSize) {
        long start = (long) (pageNum - 1) * pageSize;
        if (start >= total) {
            return Collections.emptyList();
        }
        List<Long> ids = redisService.getCacheListByRange(listKey, start, start + pageSize - 1, Long.class);
        return ids == null ? Collections.emptyList() : ids;
    }

    // 批量读取已发布竞赛详情，缓存缺失时回源数据库并回填；结果按入参顺序排列，不存在或未发布的竞赛跳过
    private Map<Long, TbExam> loadExamDetails(List<Long> examIds) {
        List<String> detailKeys = examIds.stream()
                .map(id -> CacheConstants.EXAM_DETAIL_KEY + id)
                .collect(Collectors.toList());
        List<TbExam> cached = redisService.multiGetCacheObject(detailKeys, TbExam.class);

        Map<Long, TbExam> result = new LinkedHashMap<>(examIds.size());
        for (int i = 0; i < examIds.size(); i++) {
            Long examId = examIds.get(i);
            TbExam exam = i < cached.size() ? cached.get(i) : null;
            if (exam == null) {
                exam = examMapper.selectById(examId);
                if (exam != null && ExamPublishStatusEnum.PUBLISHED.getCode().equals(exam.getStatus())) {
                    redisService.setCacheObject(CacheConstants.EXAM_DETAIL_KEY + examId, exam);
                }
            }
            if (exam != null && ExamPublishStatusEnum.PUBLISHED.getCode().equals(exam.getStatus())) {
                result.put(examId, exam);
            }
        }
        return result;
    }

    // 构造空分页结果（保留总数）
    private <T> Page<T> emptyPage(int pageNum, int pageSize, long total) {
        Page<T> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        return page;
    }

    // 将当前页数据包装为分页结果
    private <T> Page<T> toPage(List<T> rows, int pageNum, int pageSize, long total) {
        Page<T> page = emptyPage(pageNum, pageSize, total);
        page.addAll(rows);
        return page;
    }
}
