package cn.nuonuoya.system.cache;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.enums.ExamStatus;
import cn.nuonuoya.system.mapper.ExamMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 竞赛缓存管理组件
@Component
public class ExamCacheManager {

    @Autowired
    private RedisService redisService;

    @Autowired
    private ExamMapper examMapper;

    // 缓存单条竞赛详情
    public void saveExamDetail(TbExam exam) {
        if (exam == null || exam.getExamId() == null) {
            return;
        }
        redisService.setCacheObject(CacheConstants.EXAM_DETAIL_KEY + exam.getExamId(), exam);
    }

    // 删除单条竞赛详情缓存
    public void deleteExamDetail(Long examId) {
        if (examId == null) {
            return;
        }
        redisService.deleteObject(CacheConstants.EXAM_DETAIL_KEY + examId);
    }

    // 重构未完赛竞赛列表缓存（按开赛时间升序）
    public void refreshUnfinishList() {
        LocalDateTime now = LocalDateTime.now();
        List<TbExam> list = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                .eq(TbExam::getStatus, ExamStatus.PUBLISHED.getValue())
                .gt(TbExam::getEndTime, now)
                .orderByAsc(TbExam::getStartTime));
        redisService.deleteObject(CacheConstants.EXAM_UNFINISH_LIST_KEY);
        if (CollUtil.isNotEmpty(list)) {
            List<Long> idList = list.stream().map(TbExam::getExamId).collect(Collectors.toList());
            redisService.rightPushAll(CacheConstants.EXAM_UNFINISH_LIST_KEY, idList);
            for (TbExam exam : list) {
                saveExamDetail(exam);
            }
        }
    }

    // 重构历史竞赛列表缓存（按结束时间降序）
    public void refreshHistoryList() {
        LocalDateTime now = LocalDateTime.now();
        List<TbExam> list = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                .eq(TbExam::getStatus, ExamStatus.PUBLISHED.getValue())
                .le(TbExam::getEndTime, now)
                .orderByDesc(TbExam::getEndTime));
        redisService.deleteObject(CacheConstants.EXAM_HISTORY_LIST_KEY);
        if (CollUtil.isNotEmpty(list)) {
            List<Long> idList = list.stream().map(TbExam::getExamId).collect(Collectors.toList());
            redisService.rightPushAll(CacheConstants.EXAM_HISTORY_LIST_KEY, idList);
            for (TbExam exam : list) {
                saveExamDetail(exam);
            }
        }
    }

    // 从缓存中完全移除指定竞赛
    public void removeExam(Long examId) {
        if (examId == null) {
            return;
        }
        deleteExamDetail(examId);
        redisService.removeForList(CacheConstants.EXAM_UNFINISH_LIST_KEY, examId);
        redisService.removeForList(CacheConstants.EXAM_HISTORY_LIST_KEY, examId);
    }

    // 全量预热/同步所有已发布竞赛缓存
    public void syncAllExamCache() {
        refreshUnfinishList();
        refreshHistoryList();
    }
}
