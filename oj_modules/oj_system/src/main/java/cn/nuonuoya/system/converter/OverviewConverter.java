package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamStatVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitStatBaseVO;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.enums.OverviewTrendRange;
import cn.nuonuoya.system.vo.OverviewExamVO;
import cn.nuonuoya.system.vo.OverviewPeriodVO;
import cn.nuonuoya.system.vo.OverviewQuestionVO;
import cn.nuonuoya.system.vo.OverviewTrendVO;
import cn.nuonuoya.system.vo.OverviewVO;
import cn.nuonuoya.system.vo.SubmitStatBaseVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.ToIntFunction;

// 数据概览对象转换器
public class OverviewConverter {

    // 趋势横轴的日期格式
    private static final String TREND_LABEL_PATTERN = "MM.dd";

    private OverviewConverter() {
    }

    // C端统计结果转换为管理端视图（题目标题与难度由调用方补充）
    public static OverviewVO toVO(FriendOverviewVO source) {
        OverviewVO vo = new OverviewVO();
        vo.setToday(toStat(source.getToday(), OverviewPeriodVO.class));
        vo.setWeek(toStat(source.getWeek(), OverviewPeriodVO.class));
        vo.setHardQuestions(toStatList(source.getHardQuestions(), OverviewQuestionVO.class));
        return vo;
    }

    // 一段日期内的每日统计累加为趋势中的一个点（按天的点只显示当天，按周、半月的点显示起止）
    public static OverviewTrendVO toTrendVO(OverviewTrendRange.Bucket bucket, boolean daily, List<FriendDailyStatVO> days) {
        OverviewTrendVO vo = new OverviewTrendVO();
        vo.setStartDate(bucket.start());
        vo.setEndDate(bucket.end());
        String start = LocalDateTimeUtil.format(bucket.start(), TREND_LABEL_PATTERN);
        vo.setLabel(daily ? start : start + "-" + LocalDateTimeUtil.format(bucket.end(), TREND_LABEL_PATTERN));
        vo.setSubmitCount(sum(days, FriendSubmitStatBaseVO::getSubmitCount));
        vo.setJudgedCount(sum(days, FriendSubmitStatBaseVO::getJudgedCount));
        vo.setPassCount(sum(days, FriendSubmitStatBaseVO::getPassCount));
        vo.setPassRate(percent(vo.getPassCount(), vo.getJudgedCount()));
        return vo;
    }

    // 竞赛信息与人数合并为视图（没有报名、没有提交的竞赛人数保持默认 0）
    public static OverviewExamVO toExamVO(TbExam exam, FriendExamStatVO stat) {
        OverviewExamVO vo = BeanUtil.copyProperties(exam, OverviewExamVO.class);
        if (stat != null) {
            BeanUtil.copyProperties(stat, vo);
        }
        vo.setFinished(exam.getEndTime() != null && !LocalDateTime.now().isBefore(exam.getEndTime()));
        return vo;
    }

    // 百分比：部分 ÷ 整体，保留一位小数；整体为 0 时为空（通过率、参赛率共用）
    public static Double percent(Integer part, Integer whole) {
        if (whole == null || whole == 0) {
            return null;
        }
        int count = part == null ? 0 : part;
        return Math.round(count * 1000.0 / whole) / 10.0;
    }

    // 计数类统计转换为视图并补通过率
    private static <T extends SubmitStatBaseVO> T toStat(FriendSubmitStatBaseVO source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T vo = BeanUtil.copyProperties(source, targetClass);
        vo.setPassRate(percent(source.getPassCount(), source.getJudgedCount()));
        return vo;
    }

    // 对若干天的某项计数求和
    private static int sum(List<FriendDailyStatVO> days, ToIntFunction<FriendSubmitStatBaseVO> getter) {
        return days.stream().mapToInt(getter).sum();
    }

    // 计数类统计列表转换
    private static <T extends SubmitStatBaseVO> List<T> toStatList(List<? extends FriendSubmitStatBaseVO> sourceList, Class<T> targetClass) {
        return CollUtil.emptyIfNull(sourceList).stream().map(source -> toStat(source, targetClass)).toList();
    }
}
