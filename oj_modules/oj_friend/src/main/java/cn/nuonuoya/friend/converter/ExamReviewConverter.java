package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.ai.dto.AiExamReviewDTO;
import cn.nuonuoya.api.ai.dto.AiExamReviewQuestionDTO;
import cn.nuonuoya.api.ai.vo.AiExamReviewCommentVO;
import cn.nuonuoya.api.ai.vo.AiExamReviewVO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.enums.QuestionCaseTypeEnum;
import cn.nuonuoya.friend.enums.QuestionDifficultyEnum;
import cn.nuonuoya.friend.vo.ExamReviewQuestionVO;
import cn.nuonuoya.friend.vo.ExamReviewVO;
import com.alibaba.fastjson2.JSON;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// 赛后复盘对象转换器
public class ExamReviewConverter {

    // 竞赛实体到复盘视图的映射（竞赛标题换名）
    private static final CopyOptions EXAM_TO_REVIEW = CopyOptions.create().setFieldMapping(Map.of("title", "examTitle"));

    // 题目实体到 AI 题面字段的映射
    private static final CopyOptions QUESTION_TO_AI = CopyOptions.create()
            .setFieldMapping(Map.of("title", "questionTitle", "content", "questionContent"));

    private ExamReviewConverter() {
    }

    // 组装成绩概览与逐题统计：补难度与结论描述、全场通过率，通过题数由逐题结果得出
    public static ExamReviewVO toReviewVO(TbExam exam, TbUserExam userExam, int participantCount,
                                          List<ExamReviewQuestionVO> questions) {
        ExamReviewVO vo = BeanUtil.toBean(exam, ExamReviewVO.class, EXAM_TO_REVIEW);
        BeanUtil.copyProperties(userExam, vo, "examId");
        vo.setParticipantCount(participantCount);
        for (ExamReviewQuestionVO question : questions) {
            question.setDifficultyDesc(QuestionDifficultyEnum.getDescByCode(question.getDifficulty()));
            question.setLastVerdict(verdictDesc(question.getLastJudgeStatus()));
            question.setPassRate(percent(question.getPassUsers(), question.getAttemptUsers()));
        }
        vo.setQuestions(questions);
        vo.setQuestionCount(questions.size());
        vo.setPassedCount((int) questions.stream().filter(q -> Boolean.TRUE.equals(q.getPassed())).count());
        return vo;
    }

    // 成绩概览转换为 AI 请求（逐题由调用方追加）
    public static AiExamReviewDTO toRequest(ExamReviewVO vo) {
        return BeanUtil.copyProperties(vo, AiExamReviewDTO.class, "questions");
    }

    // 一道题的统计、题面与需要点评的那次提交转换为 AI 请求（隐藏用例不带输入输出）
    public static AiExamReviewQuestionDTO toQuestionRequest(int index, ExamReviewQuestionVO stat, TbQuestion question,
                                                            TbUserSubmit focus, TbQuestionCase failCase) {
        AiExamReviewQuestionDTO dto = BeanUtil.toBean(question, AiExamReviewQuestionDTO.class, QUESTION_TO_AI);
        BeanUtil.copyProperties(stat, dto, "questionId");
        dto.setIndex(index);
        if (focus != null) {
            BeanUtil.copyProperties(focus, dto, "questionId");
            dto.setVerdict(verdictDesc(focus.getJudgeStatus()));
        }
        if (failCase != null) {
            boolean sample = Objects.equals(failCase.getIsSample(), QuestionCaseTypeEnum.SAMPLE.getCode());
            dto.setFailCaseSample(sample);
            if (sample) {
                dto.setSampleInput(failCase.getDisplayInput());
                dto.setSampleOutput(failCase.getDisplayOutput());
            }
        }
        return dto;
    }

    // AI 的总结与逐题点评写入复盘（点评按序号对应，没有提交的题不写）
    public static void fillComments(ExamReviewVO vo, AiExamReviewVO result) {
        BeanUtil.copyProperties(result, vo, "questions");
        Map<Integer, String> commentByIndex = CollUtil.emptyIfNull(result.getQuestions()).stream()
                .collect(Collectors.toMap(AiExamReviewCommentVO::getIndex, AiExamReviewCommentVO::getComment, (first, second) -> first));
        for (int i = 0; i < vo.getQuestions().size(); i++) {
            ExamReviewQuestionVO question = vo.getQuestions().get(i);
            if (question.getSubmitCount() > 0) {
                question.setComment(commentByIndex.get(i + 1));
            }
        }
    }

    // 复盘序列化后存表
    public static String toContent(ExamReviewVO vo) {
        return JSON.toJSONString(vo);
    }

    // 从表里读出复盘
    public static ExamReviewVO fromContent(String content) {
        return JSON.parseObject(content, ExamReviewVO.class);
    }

    // 判题结论描述（没有结论时为空）
    private static String verdictDesc(Integer judgeStatus) {
        JudgeStatusEnum status = JudgeStatusEnum.getByCode(judgeStatus);
        return status == null ? null : status.getDesc();
    }

    // 百分比：部分 ÷ 整体，保留一位小数；整体为 0 时为空
    private static Double percent(Integer part, Integer whole) {
        if (whole == null || whole == 0) {
            return null;
        }
        return Math.round((part == null ? 0 : part) * 1000.0 / whole) / 10.0;
    }
}
