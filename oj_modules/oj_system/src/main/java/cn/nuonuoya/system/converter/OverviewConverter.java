package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.friend.vo.FriendExamStatVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitStatBaseVO;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.vo.OverviewExamVO;
import cn.nuonuoya.system.vo.OverviewPeriodVO;
import cn.nuonuoya.system.vo.OverviewQuestionVO;
import cn.nuonuoya.system.vo.OverviewTrendVO;
import cn.nuonuoya.system.vo.OverviewVO;
import cn.nuonuoya.system.vo.SubmitStatBaseVO;

import java.time.LocalDateTime;
import java.util.List;

// 数据概览对象转换器
public class OverviewConverter {

    private OverviewConverter() {
    }

    // C端统计结果转换为管理端视图（题目标题、难度与竞赛信息由调用方补充）
    public static OverviewVO toVO(FriendOverviewVO source) {
        OverviewVO vo = new OverviewVO();
        vo.setToday(toStat(source.getToday(), OverviewPeriodVO.class));
        vo.setWeek(toStat(source.getWeek(), OverviewPeriodVO.class));
        vo.setTrend(toStatList(source.getDailyTrend(), OverviewTrendVO.class));
        vo.setHardQuestions(toStatList(source.getHardQuestions(), OverviewQuestionVO.class));
        return vo;
    }

    // 竞赛参与统计与竞赛信息合并为视图
    public static OverviewExamVO toExamVO(FriendExamStatVO stat, TbExam exam) {
        OverviewExamVO vo = BeanUtil.copyProperties(stat, OverviewExamVO.class);
        BeanUtil.copyProperties(exam, vo);
        vo.setFinished(exam.getEndTime() != null && !LocalDateTime.now().isBefore(exam.getEndTime()));
        return vo;
    }

    // 通过率：通过数 ÷ 已出结论数，百分比保留一位小数；没有已出结论的提交时为空
    public static Double passRate(Integer passCount, Integer judgedCount) {
        if (judgedCount == null || judgedCount == 0) {
            return null;
        }
        int pass = passCount == null ? 0 : passCount;
        return Math.round(pass * 1000.0 / judgedCount) / 10.0;
    }

    // 计数类统计转换为视图并补通过率
    private static <T extends SubmitStatBaseVO> T toStat(FriendSubmitStatBaseVO source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T vo = BeanUtil.copyProperties(source, targetClass);
        vo.setPassRate(passRate(source.getPassCount(), source.getJudgedCount()));
        return vo;
    }

    // 计数类统计列表转换
    private static <T extends SubmitStatBaseVO> List<T> toStatList(List<? extends FriendSubmitStatBaseVO> sourceList, Class<T> targetClass) {
        return CollUtil.emptyIfNull(sourceList).stream().map(source -> toStat(source, targetClass)).toList();
    }
}
