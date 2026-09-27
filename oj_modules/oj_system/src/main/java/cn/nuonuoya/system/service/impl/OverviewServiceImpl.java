package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.friend.vo.FriendExamStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.system.client.FriendStatsClient;
import cn.nuonuoya.system.converter.OverviewConverter;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.dto.OverviewExamQueryDTO;
import cn.nuonuoya.system.enums.ExamStatus;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 数据概览业务实现（口径见 D-016、D-019）
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

    // 近 N 天每日提交趋势（天数范围已由参数校验保证）
    @Override
    public List<OverviewTrendVO> getTrend(Integer days) {
        return OverviewConverter.toTrendList(friendStatsClient.getTrend(days));
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

        OverviewExamSummaryVO vo = new OverviewExamSummaryVO();
        if (examIds.isEmpty()) {
            vo.setEnrollCount(0);
            vo.setParticipantCount(0);
            vo.setTotal(0L);
            vo.setRows(Collections.emptyList());
            return vo;
        }
        FriendExamSummaryVO summary = friendStatsClient.getExamSummary(examIds);
        vo.setEnrollCount(summary.getEnrollCount());
        vo.setParticipantCount(summary.getParticipantCount());
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

    // 批量补题目标题与难度
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
                vo.setTitle(question.getTitle());
                vo.setDifficulty(question.getDifficulty());
                vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(question.getDifficulty()));
            }
        }
    }
}
