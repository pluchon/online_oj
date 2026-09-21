package cn.nuonuoya.friend.cache;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.friend.constants.FriendCacheConstants;
import cn.nuonuoya.friend.domain.TbExamQuestion;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.mapper.ExamQuestionMapper;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.vo.QuestionPreNextVO;
import cn.nuonuoya.redis.service.RedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// 题目顺序列表缓存管理组件
@Component
public class QuestionCacheManager {

    @Autowired
    private RedisService redisService;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    // 解析缓存Key（区分日常题库与特定竞赛）
    private String getListKey(Long examId) {
        return examId == null ? FriendCacheConstants.QUESTION_LIST_KEY : (FriendCacheConstants.EXAM_QUESTION_LIST_KEY + examId);
    }

    // 初始化题目顺序列表缓存
    public void initListCache(Long examId) {
        String listKey = getListKey(examId);
        redisService.deleteObject(listKey);

        if (examId == null) {
            // 普通日常题库：按题目ID升序排布
            List<TbQuestion> list = questionMapper.selectList(new LambdaQueryWrapper<TbQuestion>()
                    .select(TbQuestion::getQuestionId)
                    .orderByAsc(TbQuestion::getQuestionId));
            if (CollUtil.isNotEmpty(list)) {
                List<Long> idList = list.stream().map(TbQuestion::getQuestionId).collect(Collectors.toList());
                redisService.rightPushAll(listKey, idList);
            }
        } else {
            // 竞赛题目列表：按竞赛题目关联表的question_order升序排布
            List<TbExamQuestion> list = examQuestionMapper.selectList(new LambdaQueryWrapper<TbExamQuestion>()
                    .eq(TbExamQuestion::getExamId, examId)
                    .orderByAsc(TbExamQuestion::getQuestionOrder));
            if (CollUtil.isNotEmpty(list)) {
                List<Long> idList = list.stream().map(TbExamQuestion::getQuestionId).collect(Collectors.toList());
                redisService.rightPushAll(listKey, idList);
            }
        }
    }

    // 清除题目顺序列表缓存（下次访问时自动重建）
    public void evictListCache(Long examId) {
        redisService.deleteObject(getListKey(examId));
    }

    // 获取题目列表首道题目的ID
    public Long getFirstQuestionId(Long examId) {
        String listKey = getListKey(examId);
        if (Boolean.FALSE.equals(redisService.hasKey(listKey))) {
            initListCache(examId);
        }
        return redisService.getListByIndex(listKey, 0, Long.class);
    }

    // 根据当前题目ID获取上一题与下一题ID
    public QuestionPreNextVO getPreAndNextQuestionId(Long questionId, Long examId) {
        if (questionId == null) {
            return new QuestionPreNextVO();
        }

        String listKey = getListKey(examId);
        if (Boolean.FALSE.equals(redisService.hasKey(listKey))) {
            initListCache(examId);
        }

        // 查询当前题目在Redis List中的下标
        Long index = redisService.indexOfList(listKey, questionId);
        // 若缓存未命中（可能新录入题目），触发自愈刷新重试
        if (index == null || index < 0) {
            initListCache(examId);
            index = redisService.indexOfList(listKey, questionId);
        }

        QuestionPreNextVO vo = new QuestionPreNextVO();
        if (index != null && index >= 0) {
            Long size = redisService.getListSize(listKey);
            if (index > 0) {
                vo.setPreQuestionId(redisService.getListByIndex(listKey, index - 1, Long.class));
            }
            if (size != null && index < size - 1) {
                vo.setNextQuestionId(redisService.getListByIndex(listKey, index + 1, Long.class));
            }
        }
        return vo;
    }
}
