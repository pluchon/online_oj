package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 数据概览趋势的时间范围：统计多长时间、每个点代表多长一段（D-019）
@AllArgsConstructor
@Getter
public enum OverviewTrendRange {

    // 近七天，每天一个点
    WEEK(true) {
        @Override
        public List<Bucket> buckets(LocalDate today) {
            return days(today, 7);
        }
    },
    // 近十四天，每天一个点
    TWO_WEEKS(true) {
        @Override
        public List<Bucket> buckets(LocalDate today) {
            return days(today, 14);
        }
    },
    // 近一个月（30 天），每天一个点
    MONTH(true) {
        @Override
        public List<Bucket> buckets(LocalDate today) {
            return days(today, 30);
        }
    },
    // 近半年，每周一个点（26 周，最后一周截止今天）
    HALF_YEAR(false) {
        @Override
        public List<Bucket> buckets(LocalDate today) {
            List<Bucket> buckets = new ArrayList<>(HALF_YEAR_WEEKS);
            for (int i = HALF_YEAR_WEEKS - 1; i >= 0; i--) {
                LocalDate end = today.minusWeeks(i);
                buckets.add(new Bucket(end.minusDays(6), end));
            }
            return buckets;
        }
    },
    // 近一年，每半个月一个点（每月 1 ~ 15 日、16 日 ~ 月末，共 24 段，当前半月截止今天）
    YEAR(false) {
        @Override
        public List<Bucket> buckets(LocalDate today) {
            List<Bucket> buckets = new ArrayList<>(YEAR_HALF_MONTHS);
            LocalDate start = today.getDayOfMonth() > HALF_MONTH_DAY ? today.withDayOfMonth(HALF_MONTH_DAY + 1) : today.withDayOfMonth(1);
            for (int i = 0; i < YEAR_HALF_MONTHS; i++) {
                LocalDate end = start.getDayOfMonth() == 1 ? start.withDayOfMonth(HALF_MONTH_DAY) : start.withDayOfMonth(start.lengthOfMonth());
                buckets.add(0, new Bucket(start, end.isAfter(today) ? today : end));
                start = start.getDayOfMonth() == 1 ? start.minusMonths(1).withDayOfMonth(HALF_MONTH_DAY + 1) : start.withDayOfMonth(1);
            }
            return buckets;
        }
    };

    // 近半年的周数
    private static final int HALF_YEAR_WEEKS = 26;

    // 近一年的半月段数
    private static final int YEAR_HALF_MONTHS = 24;

    // 上半月的最后一天
    private static final int HALF_MONTH_DAY = 15;

    // 是否每天一个点（否则每个点是一段日期）
    private final boolean daily;

    // 按时间先后排列的各段日期（首尾都包含）
    public abstract List<Bucket> buckets(LocalDate today);

    // 截止今天的最近若干天，每天一段
    private static List<Bucket> days(LocalDate today, int count) {
        List<Bucket> buckets = new ArrayList<>(count);
        for (int i = count - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            buckets.add(new Bucket(date, date));
        }
        return buckets;
    }

    // 一段日期（首尾都包含）
    public record Bucket(LocalDate start, LocalDate end) {
    }
}
