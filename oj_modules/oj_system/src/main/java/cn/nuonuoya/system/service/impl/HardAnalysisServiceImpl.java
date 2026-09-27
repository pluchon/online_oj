package cn.nuonuoya.system.service.impl;

import cn.nuonuoya.api.ai.dto.AiHardAnalysisDTO;
import cn.nuonuoya.api.ai.vo.AiHardAnalysisVO;
import cn.nuonuoya.api.ai.vo.AiHardSuspectVO;
import cn.nuonuoya.api.friend.vo.FriendHardAnalysisVO;
import cn.nuonuoya.api.friend.vo.FriendHardQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitStatBaseVO;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.system.client.AiClient;
import cn.nuonuoya.system.client.FriendStatsClient;
import cn.nuonuoya.system.constants.SystemCacheConstants;
import cn.nuonuoya.system.converter.HardAnalysisConverter;
import cn.nuonuoya.system.converter.OverviewConverter;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.mapper.QuestionCaseMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.service.HardAnalysisService;
import cn.nuonuoya.system.vo.OverviewHardAnalysisVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 难题分析实现（D-021）：数字由 oj-friend 的 SQL 统计，AI 只写结论与可疑题判断；结果缓存不过期，重新分析时覆盖
@Service
public class HardAnalysisServiceImpl implements HardAnalysisService {

    // 分析所需的最少题数（每题已出结论的提交满 5 条）
    private static final int MIN_QUESTION_COUNT = 3;

    // 通过率最低、最高的标签各展示的个数
    private static final int TAG_LIMIT = 5;

    // 出题质量提醒最多列出的题数
    private static final int MAX_SUSPECTS = 5;

    // 失败集中在单个用例的判定：该用例的失败数下限
    private static final int SUSPECT_MIN_CASE_FAILS = 3;

    // 失败集中在单个用例的判定：该用例占失败的百分比下限
    private static final double SUSPECT_CASE_SHARE = 60;

    // 每道可疑题抽查的失败代码份数
    private static final int SAMPLES_PER_SUSPECT = 3;

    @Autowired
    private FriendStatsClient friendStatsClient;

    @Autowired
    private AiClient aiClient;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionCaseMapper questionCaseMapper;

    @Autowired
    private RedisService redisService;

    // 读取缓存的分析结果
    @Override
    public OverviewHardAnalysisVO getLatest() {
        return redisService.getCacheObject(SystemCacheConstants.HARD_ANALYSIS_KEY, OverviewHardAnalysisVO.class);
    }

    // 统计 → 挑可疑题并收集材料 → AI 归纳 → 组装并缓存；题数不够时直接返回范围，不调用 AI、不覆盖缓存
    @Override
    public OverviewHardAnalysisVO analyze() {
        FriendHardAnalysisVO stats = friendStatsClient.getHardAnalysis();
        OverviewHardAnalysisVO vo = new OverviewHardAnalysisVO();
        vo.setMinQuestionCount(MIN_QUESTION_COUNT);
        vo.setQuestionCount(stats.getQuestions().size());
        vo.setJudgedCount(stats.getQuestions().stream().mapToInt(FriendSubmitStatBaseVO::getJudgedCount).sum());
        if (vo.getQuestionCount() < MIN_QUESTION_COUNT) {
            return vo;
        }
        vo.setWeakTags(HardAnalysisConverter.toWeakTags(stats.getTags(), TAG_LIMIT));
        vo.setStrongTags(HardAnalysisConverter.toStrongTags(stats.getTags(), TAG_LIMIT));
        vo.setVerdicts(HardAnalysisConverter.toVerdictList(stats.getVerdicts()));
        List<HardAnalysisConverter.Suspect> suspects = collectSuspects(stats.getQuestions());

        AiHardAnalysisDTO request = HardAnalysisConverter.toRequest(vo);
        for (int i = 0; i < suspects.size(); i++) {
            request.getSuspects().add(HardAnalysisConverter.toSuspectRequest(i + 1, suspects.get(i)));
        }
        AiHardAnalysisVO result = aiClient.analyzeHardQuestions(request);

        Map<Integer, String> commentByIndex = result.getSuspects().stream()
                .collect(Collectors.toMap(AiHardSuspectVO::getIndex, AiHardSuspectVO::getComment, (first, second) -> first));
        for (int i = 0; i < suspects.size(); i++) {
            vo.getSuspects().add(HardAnalysisConverter.toSuspectVO(suspects.get(i), commentByIndex.get(i + 1)));
        }
        HardAnalysisConverter.fillSummary(vo, result);
        vo.setSufficient(true);
        vo.setGeneratedTime(LocalDateTime.now());
        redisService.setCacheObject(SystemCacheConstants.HARD_ANALYSIS_KEY, vo);
        return vo;
    }

    // 挑出可疑题（失败集中在单个用例或有成立的申诉；申诉多的、失败越集中的在前）并收集题面、用例与失败代码
    private List<HardAnalysisConverter.Suspect> collectSuspects(List<FriendHardQuestionStatVO> questions) {
        List<FriendHardQuestionStatVO> candidates = questions.stream()
                .filter(stat -> isCaseConcentrated(stat) || stat.getUpheldAppealCount() > 0)
                .sorted(Comparator.comparing(FriendHardQuestionStatVO::getUpheldAppealCount).reversed()
                        .thenComparing(this::caseShare, Comparator.reverseOrder()))
                .limit(MAX_SUSPECTS)
                .toList();
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, TbQuestion> questionById = questionMapper.selectByIds(candidates.stream()
                        .map(FriendHardQuestionStatVO::getQuestionId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(TbQuestion::getQuestionId, Function.identity()));

        List<HardAnalysisConverter.Suspect> suspects = new ArrayList<>();
        for (FriendHardQuestionStatVO stat : candidates) {
            TbQuestion question = questionById.get(stat.getQuestionId());
            if (question == null) {
                continue;
            }
            Integer caseIndex = null;
            TbQuestionCase failCase = null;
            if (isCaseConcentrated(stat)) {
                List<TbQuestionCase> cases = questionCaseMapper.selectJudgeOrdered(stat.getQuestionId());
                for (int i = 0; i < cases.size(); i++) {
                    if (cases.get(i).getCaseId().equals(stat.getTopCaseId())) {
                        caseIndex = i + 1;
                        failCase = cases.get(i);
                        break;
                    }
                }
            }
            // 失败集中的用例已被删掉、也没有成立的申诉：多半已经修过，不再提醒
            if (failCase == null && stat.getUpheldAppealCount() == 0) {
                continue;
            }
            Long sampleCaseId = failCase == null ? null : failCase.getCaseId();
            suspects.add(new HardAnalysisConverter.Suspect(stat, question, caseIndex, failCase, caseShare(stat),
                    friendStatsClient.getFailedSamples(stat.getQuestionId(), sampleCaseId, SAMPLES_PER_SUSPECT)));
        }
        return suspects;
    }

    // 失败是否集中在单个用例
    private boolean isCaseConcentrated(FriendHardQuestionStatVO stat) {
        return stat.getTopCaseId() != null
                && stat.getTopCaseFailCount() >= SUSPECT_MIN_CASE_FAILS
                && caseShare(stat) >= SUSPECT_CASE_SHARE;
    }

    // 失败最集中的用例占失败的百分比（没有记录用例的失败时为 0）
    private double caseShare(FriendHardQuestionStatVO stat) {
        Double share = OverviewConverter.percent(stat.getTopCaseFailCount(), stat.getCaseFailCount());
        return share == null ? 0 : share;
    }
}
