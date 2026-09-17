package cn.nuonuoya.friend.cache;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import cn.nuonuoya.friend.converter.ExamConverter;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.vo.ExamVO;
import cn.nuonuoya.friend.vo.UserExamVO;
import cn.nuonuoya.redis.service.RedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// C端竞赛缓存管理组件
@Component
public class ExamCacheManager {

    // 已发布状态常量
    private static final int STATUS_PUBLISHED = 1;

    // 未完赛类型标识
    public static final int TYPE_UNFINISH = 0;

    // 历史竞赛类型标识
    public static final int TYPE_HISTORY = 1;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    // 从Redis缓存中分页获取竞赛列表
    public List<ExamVO> getExamList(int type, int pageNum, int pageSize) {
        String listKey = getListKey(type);
        // 若缓存不存在（老数据兜底），则从数据库初始化
        if (Boolean.FALSE.equals(redisService.hasKey(listKey))) {
            initCache(type);
        }

        Long total = redisService.getListSize(listKey);
        if (total == null || total == 0) {
            Page<ExamVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setTotal(0);
            return emptyPage;
        }

        long start = (long) (pageNum - 1) * pageSize;
        long end = start + pageSize - 1;
        if (start >= total) {
            Page<ExamVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setTotal(total);
            return emptyPage;
        }

        // 获取当前分页的竞赛ID集合
        List<Long> idList = redisService.getCacheListByRange(listKey, start, end, Long.class);
        if (CollUtil.isEmpty(idList)) {
            Page<ExamVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setTotal(total);
            return emptyPage;
        }

        // 批量获取竞赛详情
        List<String> detailKeys = idList.stream()
                .map(id -> CacheConstants.EXAM_DETAIL_KEY + id)
                .collect(Collectors.toList());
        List<TbExam> examList = redisService.multiGetCacheObject(detailKeys, TbExam.class);

        // 详情兜底检查：若存在缺失的详情则从DB回补并写入缓存
        List<TbExam> finalList = new ArrayList<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            TbExam exam = (examList != null && i < examList.size()) ? examList.get(i) : null;
            if (exam == null) {
                Long examId = idList.get(i);
                exam = examMapper.selectById(examId);
                if (exam != null) {
                    redisService.setCacheObject(CacheConstants.EXAM_DETAIL_KEY + examId, exam);
                }
            }
            if (exam != null) {
                finalList.add(exam);
            }
        }

        // 动态计算开赛状态并转换为VO
        List<ExamVO> voList = ExamConverter.toVOList(finalList);

        // 获取当前请求登录用户并填充是否已报名状态
        Long currentUserId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        populateIsEnter(voList, currentUserId);

        // 组装 PageHelper Page 对象以保留物理分页元数据
        Page<ExamVO> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.addAll(voList);
        return page;
    }

    // 填充列表VO中当前用户的报名状态
    public void populateIsEnter(List<ExamVO> voList, Long userId) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        if (userId == null) {
            for (ExamVO vo : voList) {
                vo.setIsEnter(false);
            }
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
        String userExamListKey = CacheConstants.USER_EXAM_LIST_KEY + userId;
        if (Boolean.FALSE.equals(redisService.hasKey(userExamListKey))) {
            initUserExamCache(userId);
        }
        List<Long> idList = redisService.getCacheListByRange(userExamListKey, 0, -1, Long.class);
        return CollUtil.isNotEmpty(idList) ? new HashSet<>(idList) : Collections.emptySet();
    }

    // 用户报名成功后实时向Redis追加记录
    public void addUserExamCache(Long userId, Long examId) {
        if (userId == null || examId == null) {
            return;
        }
        String userExamListKey = CacheConstants.USER_EXAM_LIST_KEY + userId;
        if (Boolean.TRUE.equals(redisService.hasKey(userExamListKey))) {
            redisService.leftPushForList(userExamListKey, examId);
        } else {
            initUserExamCache(userId);
        }
    }

    // 老数据兜底：从数据库初始化指定用户的已报名竞赛缓存
    public synchronized void initUserExamCache(Long userId) {
        if (userId == null) {
            return;
        }
        String userExamListKey = CacheConstants.USER_EXAM_LIST_KEY + userId;
        List<TbUserExam> list = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                .select(TbUserExam::getExamId)
                .eq(TbUserExam::getUserId, userId)
                .orderByDesc(TbUserExam::getCreateTime));
        redisService.deleteObject(userExamListKey);
        if (CollUtil.isNotEmpty(list)) {
            List<Long> idList = list.stream().map(TbUserExam::getExamId).collect(Collectors.toList());
            redisService.rightPushAll(userExamListKey, idList);
        }
    }

    // 分页获取“我的竞赛”列表
    public List<UserExamVO> getMyExamList(Long userId, int pageNum, int pageSize) {
        if (userId == null) {
            Page<UserExamVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setTotal(0);
            return emptyPage;
        }
        String listKey = CacheConstants.USER_EXAM_LIST_KEY + userId;
        if (Boolean.FALSE.equals(redisService.hasKey(listKey))) {
            initUserExamCache(userId);
        }

        Long total = redisService.getListSize(listKey);
        if (total == null || total == 0) {
            Page<UserExamVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setTotal(0);
            return emptyPage;
        }

        long start = (long) (pageNum - 1) * pageSize;
        long end = start + pageSize - 1;
        if (start >= total) {
            Page<UserExamVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setTotal(total);
            return emptyPage;
        }

        List<Long> examIdList = redisService.getCacheListByRange(listKey, start, end, Long.class);
        if (CollUtil.isEmpty(examIdList)) {
            Page<UserExamVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setTotal(total);
            return emptyPage;
        }

        // 批量查竞赛详情（复用 exam:detail 缓存）
        List<String> detailKeys = examIdList.stream()
                .map(id -> CacheConstants.EXAM_DETAIL_KEY + id)
                .collect(Collectors.toList());
        List<TbExam> examList = redisService.multiGetCacheObject(detailKeys, TbExam.class);
        Map<Long, TbExam> examMap = new HashMap<>();
        for (int i = 0; i < examIdList.size(); i++) {
            Long examId = examIdList.get(i);
            TbExam exam = (examList != null && i < examList.size()) ? examList.get(i) : null;
            if (exam == null) {
                exam = examMapper.selectById(examId);
                if (exam != null) {
                    redisService.setCacheObject(CacheConstants.EXAM_DETAIL_KEY + examId, exam);
                }
            }
            if (exam != null) {
                examMap.put(examId, exam);
            }
        }

        // 批量获取用户竞赛得分与排名记录
        List<TbUserExam> userExamRecords = userExamMapper.selectList(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getUserId, userId)
                .in(TbUserExam::getExamId, examIdList));
        Map<Long, TbUserExam> recordMap = CollUtil.isNotEmpty(userExamRecords)
                ? userExamRecords.stream().collect(Collectors.toMap(TbUserExam::getExamId, u -> u, (k1, k2) -> k1))
                : Collections.emptyMap();

        List<UserExamVO> voList = new ArrayList<>(examIdList.size());
        for (Long examId : examIdList) {
            TbExam exam = examMap.get(examId);
            if (exam != null) {
                TbUserExam ue = recordMap.get(examId);
                voList.add(ExamConverter.toUserExamVO(exam, ue));
            }
        }

        Page<UserExamVO> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.addAll(voList);
        return page;
    }

    // 老数据兜底：从数据库初始化指定类型的竞赛列表缓存
    public synchronized void initCache(int type) {
        String listKey = getListKey(type);
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<TbExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbExam::getStatus, STATUS_PUBLISHED);
        if (type == TYPE_UNFINISH) {
            wrapper.gt(TbExam::getEndTime, now);
            wrapper.orderByAsc(TbExam::getStartTime);
        } else {
            wrapper.le(TbExam::getEndTime, now);
            wrapper.orderByDesc(TbExam::getEndTime);
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

    // 根据分类获取对应的Redis List Key
    private String getListKey(int type) {
        return type == TYPE_HISTORY ? CacheConstants.EXAM_HISTORY_LIST_KEY : CacheConstants.EXAM_UNFINISH_LIST_KEY;
    }
}
