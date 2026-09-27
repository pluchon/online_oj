package cn.nuonuoya.friend.service.impl;

import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamStatVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.api.friend.vo.FriendPeriodStatVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitStatBaseVO;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.enums.ExamPublishStatusEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.StatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

// 统计业务实现（口径见 D-016：通过率分母为已出结论的提交，活跃用户为有提交的去重用户）
@Service
public class StatsServiceImpl implements StatsService {

    // 趋势统计的天数（含今日）
    private static final int TREND_DAYS = 7;

    // 进入难题榜所需的已出结论提交数
    private static final int HARD_QUESTION_MIN_JUDGED = 5;

    // 难题榜题数
    private static final int HARD_QUESTION_LIMIT = 5;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    // 汇总数据概览：每日趋势补齐没有提交的日子，今日与近 7 天由趋势累加并单独统计去重活跃用户
    @Override
    public FriendOverviewVO getOverview() {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusDays(TREND_DAYS - 1);
        Map<LocalDate, FriendDailyStatVO> statByDate = userSubmitMapper.selectDailyStats(firstDay.atStartOfDay(),
                        SubmitPassEnum.JUDGING.getCode(), SubmitPassEnum.PASS.getCode())
                .stream()
                .collect(Collectors.toMap(FriendDailyStatVO::getDate, Function.identity()));
        List<FriendDailyStatVO> trend = new ArrayList<>(TREND_DAYS);
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate date = firstDay.plusDays(i);
            trend.add(statByDate.getOrDefault(date, emptyDay(date)));
        }

        FriendOverviewVO vo = new FriendOverviewVO();
        vo.setDailyTrend(trend);
        vo.setToday(toPeriodStat(trend.subList(TREND_DAYS - 1, TREND_DAYS), today.atStartOfDay()));
        vo.setWeek(toPeriodStat(trend, firstDay.atStartOfDay()));
        vo.setHardQuestions(userSubmitMapper.selectHardQuestions(HARD_QUESTION_MIN_JUDGED, HARD_QUESTION_LIMIT,
                SubmitPassEnum.JUDGING.getCode(), SubmitPassEnum.PASS.getCode()));
        vo.setLatestExam(latestExamStat());
        return vo;
    }

    // 累加若干天的计数，并统计该时间段内的去重活跃用户
    private FriendPeriodStatVO toPeriodStat(List<FriendDailyStatVO> days, LocalDateTime startTime) {
        FriendPeriodStatVO period = new FriendPeriodStatVO();
        period.setSubmitCount(sum(days, FriendSubmitStatBaseVO::getSubmitCount));
        period.setJudgedCount(sum(days, FriendSubmitStatBaseVO::getJudgedCount));
        period.setPassCount(sum(days, FriendSubmitStatBaseVO::getPassCount));
        period.setActiveUsers(userSubmitMapper.countDistinctUsers(startTime, null));
        return period;
    }

    // 最近一场已发布且已开赛的竞赛：报名人数与实际提交人数，没有时返回 null
    private FriendExamStatVO latestExamStat() {
        PageHelper.startPage(1, 1, false);
        List<TbExam> exams = examMapper.selectList(new LambdaQueryWrapper<TbExam>()
                .select(TbExam::getExamId)
                .eq(TbExam::getStatus, ExamPublishStatusEnum.PUBLISHED.getCode())
                .le(TbExam::getStartTime, LocalDateTime.now())
                .orderByDesc(TbExam::getStartTime)
                .orderByDesc(TbExam::getExamId));
        if (exams.isEmpty()) {
            return null;
        }
        Long examId = exams.get(0).getExamId();
        FriendExamStatVO stat = new FriendExamStatVO();
        stat.setExamId(examId);
        stat.setEnrollCount(userExamMapper.selectCount(new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getExamId, examId)).intValue());
        stat.setParticipantCount(userSubmitMapper.countDistinctUsers(null, examId));
        return stat;
    }

    // 没有提交的日子
    private FriendDailyStatVO emptyDay(LocalDate date) {
        FriendDailyStatVO day = new FriendDailyStatVO();
        day.setDate(date);
        day.setSubmitCount(0);
        day.setJudgedCount(0);
        day.setPassCount(0);
        return day;
    }

    // 对若干天的某项计数求和
    private int sum(List<FriendDailyStatVO> days, ToIntFunction<FriendSubmitStatBaseVO> getter) {
        return days.stream().mapToInt(getter).sum();
    }
}
