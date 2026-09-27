package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.system.client.FriendStatsClient;
import cn.nuonuoya.system.converter.OverviewConverter;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.mapper.ExamMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.service.OverviewService;
import cn.nuonuoya.system.vo.OverviewQuestionVO;
import cn.nuonuoya.system.vo.OverviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 数据概览业务实现（口径见 D-016）
@Service
public class OverviewServiceImpl implements OverviewService {

    @Autowired
    private FriendStatsClient friendStatsClient;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamMapper examMapper;

    // 取 C 端统计，补难题的标题与难度、最近竞赛的标题与起止时间
    @Override
    public OverviewVO getOverview() {
        FriendOverviewVO stats = friendStatsClient.getOverview();
        OverviewVO vo = OverviewConverter.toVO(stats);
        fillQuestionInfo(vo.getHardQuestions());
        if (stats.getLatestExam() != null) {
            TbExam exam = examMapper.selectById(stats.getLatestExam().getExamId());
            if (exam != null) {
                vo.setLatestExam(OverviewConverter.toExamVO(stats.getLatestExam(), exam));
            }
        }
        return vo;
    }

    // 批量补题目标题与难度描述
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
                vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(question.getDifficulty()));
            }
        }
    }
}
