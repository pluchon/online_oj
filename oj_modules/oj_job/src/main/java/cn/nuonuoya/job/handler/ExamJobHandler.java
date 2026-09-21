package cn.nuonuoya.job.handler;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.job.domain.TbExam;
import cn.nuonuoya.job.enums.ExamPublishStatusEnum;
import cn.nuonuoya.job.mapper.ExamMapper;
import cn.nuonuoya.redis.service.RedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 竞赛列表与状态自动规整定时任务处理器
@Component
@Slf4j
public class ExamJobHandler {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private RedisService redisService;

    // 定时规整未完赛与历史竞赛列表缓存
    @XxlJob("examListOrganizeHandler")
    public void examListOrganizeHandler() {
        long startTime = System.currentTimeMillis();
        XxlJobHelper.log("开始执行竞赛列表缓存规整任务...");

        try {
            LocalDateTime now = LocalDateTime.now();

            // 1. 查询未完赛竞赛：已发布且结束时间晚于当前时间，按开赛时间升序排列
            List<TbExam> unFinishList = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                    .select(TbExam::getExamId, TbExam::getTitle, TbExam::getStartTime, TbExam::getEndTime, TbExam::getStatus)
                    .eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode())
                    .gt(TbExam::getEndTime, now)
                    .orderByAsc(TbExam::getStartTime));

            refreshCache(CacheConstants.EXAM_UNFINISH_LIST_KEY, unFinishList);
            int unFinishCount = CollUtil.isEmpty(unFinishList) ? 0 : unFinishList.size();
            XxlJobHelper.log("未完赛竞赛缓存已更新，共计: {} 场", unFinishCount);

            // 2. 查询历史竞赛：已发布且结束时间早于等于当前时间，按结束时间降序排列
            List<TbExam> historyList = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                    .select(TbExam::getExamId, TbExam::getTitle, TbExam::getStartTime, TbExam::getEndTime, TbExam::getStatus)
                    .eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode())
                    .le(TbExam::getEndTime, now)
                    .orderByDesc(TbExam::getEndTime));

            refreshCache(CacheConstants.EXAM_HISTORY_LIST_KEY, historyList);
            int historyCount = CollUtil.isEmpty(historyList) ? 0 : historyList.size();
            XxlJobHelper.log("历史竞赛缓存已更新，共计: {} 场", historyCount);

            long cost = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("竞赛列表缓存规整完成，总耗时: {} ms", cost);
            XxlJobHelper.handleSuccess("规整成功: 未完赛 " + unFinishCount + " 场, 历史 " + historyCount + " 场");
        } catch (Exception e) {
            log.error("竞赛列表缓存规整任务执行异常", e);
            XxlJobHelper.log("任务执行异常: " + e.getMessage());
            XxlJobHelper.handleFail("执行失败: " + e.getMessage());
        }
    }

    // 刷新单类竞赛的详情与列表缓存
    private void refreshCache(String listKey, List<TbExam> examList) {
        // 若查询结果为空，必须显式删除旧的列表Key，避免残留过期竞赛ID
        if (CollUtil.isEmpty(examList)) {
            redisService.deleteObject(listKey);
            return;
        }

        // 组装批量详情Map与ID有序列表
        Map<String, TbExam> examMap = new HashMap<>(examList.size());
        List<Long> idList = new ArrayList<>(examList.size());

        for (TbExam exam : examList) {
            examMap.put(CacheConstants.EXAM_DETAIL_KEY + exam.getExamId(), exam);
            idList.add(exam.getExamId());
        }

        // 1. 先批量写入/刷新详情缓存（MSET单次RTT）
        redisService.multiSet(examMap);

        // 2. 再重建列表缓存（先删后压栈）
        redisService.deleteObject(listKey);
        redisService.rightPushAll(listKey, idList);
    }
}
