package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.api.ai.dto.AiHardAnalysisDTO;
import cn.nuonuoya.api.ai.dto.AiHardSampleDTO;
import cn.nuonuoya.api.ai.dto.AiHardSuspectDTO;
import cn.nuonuoya.api.ai.dto.AiHardTagDTO;
import cn.nuonuoya.api.ai.dto.AiHardVerdictDTO;
import cn.nuonuoya.api.ai.vo.AiHardAnalysisVO;
import cn.nuonuoya.api.friend.vo.FriendFailedSampleVO;
import cn.nuonuoya.api.friend.vo.FriendHardQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendTagStatVO;
import cn.nuonuoya.api.friend.vo.FriendVerdictStatVO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.enums.QuestionCaseType;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.vo.OverviewHardAnalysisVO;
import cn.nuonuoya.system.vo.OverviewHardSuspectVO;
import cn.nuonuoya.system.vo.OverviewTagStatVO;
import cn.nuonuoya.system.vo.OverviewVerdictVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 难题分析对象转换器
public class HardAnalysisConverter {

    // 题目实体到 AI 题面字段的映射
    private static final CopyOptions QUESTION_TO_AI = CopyOptions.create()
            .setFieldMapping(Map.of("title", "questionTitle", "content", "questionContent"));

    // 失败样本到 AI 样本的映射
    private static final CopyOptions SAMPLE_TO_AI = CopyOptions.create()
            .setFieldMapping(Map.of("failOutput", "actualOutput"));

    private HardAnalysisConverter() {
    }

    // 一道可疑题的材料：统计、题目、失败最集中的用例（及其判题序号，失败不集中时为空）与抽查的失败代码
    public record Suspect(FriendHardQuestionStatVO stat, TbQuestion question, Integer caseIndex,
                          TbQuestionCase failCase, Double caseShare, List<FriendFailedSampleVO> samples) {
    }

    // 通过率最低的标签（入参按通过率升序；标签不多时取前一半，保证与最高的不重复）
    public static List<OverviewTagStatVO> toWeakTags(List<FriendTagStatVO> tags, int limit) {
        List<FriendTagStatVO> source = CollUtil.emptyIfNull(tags);
        return source.subList(0, weakCount(source.size(), limit)).stream()
                .map(HardAnalysisConverter::toTagVO)
                .toList();
    }

    // 通过率最高的标签（入参按通过率升序；取最低之外的末尾若干个，按通过率降序）
    public static List<OverviewTagStatVO> toStrongTags(List<FriendTagStatVO> tags, int limit) {
        List<FriendTagStatVO> source = CollUtil.emptyIfNull(tags);
        int from = Math.max(weakCount(source.size(), limit), source.size() - limit);
        return CollUtil.reverseNew(source.subList(from, source.size())).stream()
                .map(HardAnalysisConverter::toTagVO)
                .toList();
    }

    // 判题结论分布转换为视图，补结论描述与占比
    public static List<OverviewVerdictVO> toVerdictList(List<FriendVerdictStatVO> verdicts) {
        int total = CollUtil.emptyIfNull(verdicts).stream().mapToInt(FriendVerdictStatVO::getFailCount).sum();
        List<OverviewVerdictVO> list = new ArrayList<>();
        for (FriendVerdictStatVO verdict : CollUtil.emptyIfNull(verdicts)) {
            OverviewVerdictVO vo = BeanUtil.copyProperties(verdict, OverviewVerdictVO.class);
            JudgeStatusEnum status = JudgeStatusEnum.getByCode(verdict.getJudgeStatus());
            vo.setVerdict(status == null ? "其他" : status.getDesc());
            vo.setShare(OverviewConverter.percent(verdict.getFailCount(), total));
            list.add(vo);
        }
        return list;
    }

    // 分析范围与统计数字转换为 AI 请求（标签与判题结论换成请求类型，可疑题由调用方追加）
    public static AiHardAnalysisDTO toRequest(OverviewHardAnalysisVO vo) {
        AiHardAnalysisDTO request = BeanUtil.copyProperties(vo, AiHardAnalysisDTO.class,
                "weakTags", "strongTags", "verdicts", "suspects");
        request.setWeakTags(BeanUtil.copyToList(vo.getWeakTags(), AiHardTagDTO.class));
        request.setStrongTags(BeanUtil.copyToList(vo.getStrongTags(), AiHardTagDTO.class));
        request.setVerdicts(BeanUtil.copyToList(vo.getVerdicts(), AiHardVerdictDTO.class));
        return request;
    }

    // AI 的薄弱点与错误类型结论写入视图（可疑题判断由调用方按序号对应）
    public static void fillSummary(OverviewHardAnalysisVO vo, AiHardAnalysisVO result) {
        BeanUtil.copyProperties(result, vo, "suspects");
    }

    // 可疑题材料转换为 AI 请求（标题、描述换名复制；用例只带判题输入与预期输出）
    public static AiHardSuspectDTO toSuspectRequest(int index, Suspect suspect) {
        AiHardSuspectDTO dto = BeanUtil.toBean(suspect.question(), AiHardSuspectDTO.class, QUESTION_TO_AI);
        dto.setIndex(index);
        dto.setReason(reason(suspect));
        if (suspect.failCase() != null) {
            dto.setCaseIndex(suspect.caseIndex());
            dto.setCaseSample(Objects.equals(suspect.failCase().getIsSample(), QuestionCaseType.SAMPLE.getValue()));
            dto.setCaseInput(suspect.failCase().getJudgeInput());
            dto.setCaseOutput(suspect.failCase().getJudgeOutput());
        }
        dto.setSamples(BeanUtil.copyToList(suspect.samples(), AiHardSampleDTO.class, SAMPLE_TO_AI));
        return dto;
    }

    // 可疑题材料与 AI 判断组装为视图
    public static OverviewHardSuspectVO toSuspectVO(Suspect suspect, String comment) {
        OverviewHardSuspectVO vo = BeanUtil.copyProperties(suspect.question(), OverviewHardSuspectVO.class);
        vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(suspect.question().getDifficulty()));
        vo.setUpheldAppealCount(suspect.stat().getUpheldAppealCount());
        if (suspect.failCase() != null) {
            vo.setCaseIndex(suspect.caseIndex());
            vo.setCaseShare(suspect.caseShare());
        }
        vo.setComment(comment);
        return vo;
    }

    // 标签统计转换为视图并补通过率
    private static OverviewTagStatVO toTagVO(FriendTagStatVO tag) {
        OverviewTagStatVO vo = BeanUtil.copyProperties(tag, OverviewTagStatVO.class);
        vo.setPassRate(OverviewConverter.percent(tag.getPassCount(), tag.getJudgedCount()));
        return vo;
    }

    // 通过率最低的标签取几个：最多 limit 个，标签不多时取前一半（向上取整）
    private static int weakCount(int size, int limit) {
        return Math.min(limit, (size + 1) / 2);
    }

    // 被列为可疑的原因（给 AI 看的一句话）
    private static String reason(Suspect suspect) {
        List<String> parts = new ArrayList<>();
        if (suspect.failCase() != null) {
            parts.add(suspect.caseShare() + "% 的失败卡在用例 " + suspect.caseIndex());
        }
        if (suspect.stat().getUpheldAppealCount() > 0) {
            parts.add(suspect.stat().getUpheldAppealCount() + " 条申诉成立");
        }
        return String.join("，", parts);
    }
}
