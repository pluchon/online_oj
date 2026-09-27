package cn.nuonuoya.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.system.client.FriendStatsClient;
import cn.nuonuoya.system.converter.OverviewConverter;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.dto.OverviewExamQueryDTO;
import cn.nuonuoya.system.enums.ExamStatus;
import cn.nuonuoya.system.enums.OverviewTrendRange;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.mapper.ExamMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.service.OverviewService;
import cn.nuonuoya.system.vo.OverviewExamSummaryVO;
import cn.nuonuoya.system.vo.OverviewQuestionVO;
import cn.nuonuoya.system.vo.OverviewTrendVO;
import cn.nuonuoya.system.vo.OverviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// 数据概览业务实现（通过率分母为已出结论的提交，人数按用户去重）
@Service
public class OverviewServiceImpl implements OverviewService {

    @Autowired
    private FriendStatsClient friendStatsClient;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamMapper examMapper;

    // 取 C 端统计，补难题的标题与难度
    @Override
    public OverviewVO getOverview() {
        OverviewVO vo = OverviewConverter.toVO(friendStatsClient.getOverview());
        fillQuestionInfo(vo.getHardQuestions());
        return vo;
    }

    // 按时间范围分段汇总提交趋势：取覆盖全部分段的每日统计，再逐段累加
    @Override
    public List<OverviewTrendVO> getTrend(OverviewTrendRange range) {
        LocalDate today = LocalDate.now();
        List<OverviewTrendRange.Bucket> buckets = range.buckets(today);
        int days = (int) ChronoUnit.DAYS.between(buckets.get(0).start(), today) + 1;
        Map<LocalDate, FriendDailyStatVO> statByDate = friendStatsClient.getTrend(days).stream()
                .collect(Collectors.toMap(FriendDailyStatVO::getDate, Function.identity()));
        return buckets.stream()
                .map(bucket -> OverviewConverter.toTrendVO(bucket, range.isDaily(), bucket.start()
                        .datesUntil(bucket.end().plusDays(1))
                        .map(statByDate::get)
                        .filter(Objects::nonNull)
                        .toList()))
                .toList();
    }

    // 时间段内进行过的已发布竞赛：汇总人数覆盖全部竞赛，列表按开始时间倒序分页
    @Override
    public OverviewExamSummaryVO getExamSummary(OverviewExamQueryDTO queryDTO) {
        LocalDateTime periodStart = LocalDate.now().minusDays(queryDTO.getDays() - 1L).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        List<Long> examIds = examMapper.selectList(periodExamWrapper(periodStart, now).select(TbExam::getExamId))
                .stream()
                .map(TbExam::getExamId)
                .toList();

        if (examIds.isEmpty()) {
            return new OverviewExamSummaryVO();
        }
        FriendExamSummaryVO summary = friendStatsClient.getExamSummary(examIds);
        OverviewExamSummaryVO vo = BeanUtil.copyProperties(summary, OverviewExamSummaryVO.class);
        vo.setParticipationRate(OverviewConverter.percent(summary.getParticipantCount(), summary.getEnrollCount()));

        Map<Long, FriendExamStatVO> statByExam = summary.getExams().stream()
                .collect(Collectors.toMap(FriendExamStatVO::getExamId, Function.identity()));
        Page<TbExam> page = PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        examMapper.selectList(periodExamWrapper(periodStart, now)
                .select(TbExam::getExamId, TbExam::getTitle, TbExam::getStartTime, TbExam::getEndTime)
                .orderByDesc(TbExam::getStartTime)
                .orderByDesc(TbExam::getExamId));
        vo.setTotal(page.getTotal());
        vo.setRows(page.getResult().stream()
                .map(exam -> OverviewConverter.toExamVO(exam, statByExam.get(exam.getExamId())))
                .toList());
        return vo;
    }

    // 时间段内进行过的已发布竞赛：已开赛，且结束时间不早于时间段起点
    private LambdaQueryWrapper<TbExam> periodExamWrapper(LocalDateTime periodStart, LocalDateTime now) {
        return new LambdaQueryWrapper<TbExam>()
                .eq(TbExam::getStatus, ExamStatus.PUBLISHED.getValue())
                .le(TbExam::getStartTime, now)
                .ge(TbExam::getEndTime, periodStart);
    }

    // 批量补题目标题与难度（同名复制，难度描述由枚举换算）
    private void fillQuestionInfo(List<OverviewQuestionVO> questions) {
        if (CollUtil.isEmpty(questions)) {
            return;
        }
        Map<Long, TbQuestion> questionMap = questionMapper.selectList(new LambdaQueryWrapper<TbQuestion>()
                        .select(TbQuestion::getQuestionId, TbQuestion::getTitle, TbQuestion::getDifficulty)
                        .in(TbQuestion::getQuestionId, questions.stream().map(OverviewQuestionVO::getQuestionId).toList()))
                .stream()
                .collect(Collectors.toMap(TbQuestion::getQuestionId, Function.identity()));
        for (OverviewQuestionVO vo : questions) {
            TbQuestion question = questionMap.get(vo.getQuestionId());
            if (question != null) {
                BeanUtil.copyProperties(question, vo);
                vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(question.getDifficulty()));
            }
        }
    }
}
