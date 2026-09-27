package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.api.friend.vo.FriendFailedSampleVO;
import cn.nuonuoya.api.friend.vo.FriendHardAnalysisVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.api.friend.vo.FriendPeriodStatVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitStatBaseVO;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.api.friend.enums.AppealStatusEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.StatsService;
import cn.nuonuoya.security.exception.ServiceException;
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

// 统计业务实现（口径见 D-016、D-019：通过率分母为已出结论的提交，活跃用户与竞赛人数都按用户去重）
@Service
public class StatsServiceImpl implements StatsService {

    // 概览卡片统计的天数（含今日）
    private static final int WEEK_DAYS = 7;

    // 趋势最多统计的天数（管理端近一年按半月汇总，最多取 366 天的每日数据）
    private static final int MAX_TREND_DAYS = 366;

    // 进入难题榜所需的已出结论提交数
    private static final int HARD_QUESTION_MIN_JUDGED = 5;

    // 难题榜题数
    private static final int HARD_QUESTION_LIMIT = 5;

    // 失败样本单次最多取的份数
    private static final int MAX_FAILED_SAMPLES = 5;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    // 汇总数据概览：今日与近 7 天由每日统计累加并单独统计去重活跃用户
    @Override
    public FriendOverviewVO getOverview() {
        List<FriendDailyStatVO> week = buildTrend(WEEK_DAYS);
        FriendOverviewVO vo = new FriendOverviewVO();
        vo.setToday(toPeriodStat(week.subList(WEEK_DAYS - 1, WEEK_DAYS), LocalDate.now().atStartOfDay()));
        vo.setWeek(toPeriodStat(week, week.get(0).getDate().atStartOfDay()));
        vo.setHardQuestions(userSubmitMapper.selectHardQuestions(HARD_QUESTION_MIN_JUDGED, HARD_QUESTION_LIMIT,
                SubmitPassEnum.JUDGING.getCode(), SubmitPassEnum.PASS.getCode()));
        return vo;
    }

    // 近 N 天每日趋势，天数须在 1 ~ 366 之间
    @Override
    public List<FriendDailyStatVO> getTrend(Integer days) {
        if (days == null || days < 1 || days > MAX_TREND_DAYS) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        return buildTrend(days);
    }

    // 指定竞赛的报名与参赛人数：汇总按用户去重，每场各自计数（都由 SQL 算好，没有竞赛时人数为 0）
    @Override
    public FriendExamSummaryVO getExamSummary(List<Long> examIds) {
        if (CollUtil.isEmpty(examIds)) {
            return new FriendExamSummaryVO();
        }
        FriendExamSummaryVO vo = userExamMapper.selectExamSummary(examIds);
        vo.setExams(userExamMapper.selectExamStats(examIds));
        return vo;
    }

    // 难题分析统计：门槛与难题榜一致，三组数字都由 SQL 算好
    @Override
    public FriendHardAnalysisVO getHardAnalysis() {
        Integer judging = SubmitPassEnum.JUDGING.getCode();
        Integer pass = SubmitPassEnum.PASS.getCode();
        Integer notPass = SubmitPassEnum.NOT_PASS.getCode();
        FriendHardAnalysisVO vo = new FriendHardAnalysisVO();
        vo.setQuestions(userSubmitMapper.selectHardQuestionStats(HARD_QUESTION_MIN_JUDGED, judging, pass, notPass,
                AppealStatusEnum.UPHELD.getCode()));
        if (vo.getQuestions().isEmpty()) {
            return vo;
        }
        vo.setTags(userSubmitMapper.selectHardTagStats(HARD_QUESTION_MIN_JUDGED, judging, pass));
        vo.setVerdicts(userSubmitMapper.selectHardVerdictStats(HARD_QUESTION_MIN_JUDGED, judging, pass, notPass));
        return vo;
    }

    // 某题最近的未通过提交样本，份数须在 1 ~ 5 之间
    @Override
    public List<FriendFailedSampleVO> getFailedSamples(Long questionId, Long caseId, Integer limit) {
        if (questionId == null || limit == null || limit < 1 || limit > MAX_FAILED_SAMPLES) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        return userSubmitMapper.selectFailedSamples(questionId, caseId, limit, SubmitPassEnum.NOT_PASS.getCode());
    }

    // 近 N 天（含今日）每日统计，按日期升序补齐没有提交的日子
    private List<FriendDailyStatVO> buildTrend(int days) {
        LocalDate firstDay = LocalDate.now().minusDays(days - 1L);
        Map<LocalDate, FriendDailyStatVO> statByDate = userSubmitMapper.selectDailyStats(firstDay.atStartOfDay(),
                        SubmitPassEnum.JUDGING.getCode(), SubmitPassEnum.PASS.getCode())
                .stream()
                .collect(Collectors.toMap(FriendDailyStatVO::getDate, Function.identity()));
        List<FriendDailyStatVO> trend = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = firstDay.plusDays(i);
            trend.add(statByDate.getOrDefault(date, emptyDay(date)));
        }
        return trend;
    }

    // 累加若干天的计数，并统计该时间段内的去重活跃用户
    private FriendPeriodStatVO toPeriodStat(List<FriendDailyStatVO> days, LocalDateTime startTime) {
        FriendPeriodStatVO period = new FriendPeriodStatVO();
        period.setSubmitCount(sum(days, FriendSubmitStatBaseVO::getSubmitCount));
        period.setJudgedCount(sum(days, FriendSubmitStatBaseVO::getJudgedCount));
        period.setPassCount(sum(days, FriendSubmitStatBaseVO::getPassCount));
        period.setActiveUsers(userSubmitMapper.countDistinctUsers(startTime));
        return period;
    }

    // 没有提交的日子（计数默认为 0）
    private FriendDailyStatVO emptyDay(LocalDate date) {
        FriendDailyStatVO day = new FriendDailyStatVO();
        day.setDate(date);
        return day;
    }

    // 对若干天的某项计数求和
    private int sum(List<FriendDailyStatVO> days, ToIntFunction<FriendSubmitStatBaseVO> getter) {
        return days.stream().mapToInt(getter).sum();
    }
}
