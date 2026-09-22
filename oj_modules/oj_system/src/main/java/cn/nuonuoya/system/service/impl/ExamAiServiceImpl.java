package cn.nuonuoya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.ai.dto.AiExamCandidateDTO;
import cn.nuonuoya.api.ai.dto.AiExamIntentDTO;
import cn.nuonuoya.api.ai.dto.AiExamSelectDTO;
import cn.nuonuoya.api.ai.vo.AiExamIntentVO;
import cn.nuonuoya.api.friend.dto.FriendQuestionCandidateQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendQuestionCandidateVO;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.client.AiClient;
import cn.nuonuoya.system.client.FriendQuestionClient;
import cn.nuonuoya.system.dto.ExamAiPlanDTO;
import cn.nuonuoya.system.enums.ExamAiCountLevel;
import cn.nuonuoya.system.enums.ExamAiTendency;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.service.ExamAiService;
import cn.nuonuoya.system.vo.ExamAiPlanVO;
import cn.nuonuoya.system.vo.ExamAiQuestionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// AI 帮建竞赛：模型理解需求 → friend 混合检索候选 → 模型挑题 → 校验、补齐并由易到难排序
@Slf4j
@Service
public class ExamAiServiceImpl implements ExamAiService {

    // 候选池相对需要题数的倍数
    private static final int CANDIDATE_FACTOR = 3;

    // 三个难度（与配比数组顺序一致）
    private static final QuestionDifficulty[] DIFFICULTIES = {
            QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM, QuestionDifficulty.HARD
    };

    @Autowired
    private AiClient aiClient;

    @Autowired
    private FriendQuestionClient friendQuestionClient;

    // 生成竞赛名称与题目
    @Override
    public ExamAiPlanVO plan(ExamAiPlanDTO planDTO) {
        ExamAiTendency tendency = ExamAiTendency.getByCode(planDTO.getTendency());
        ExamAiCountLevel level = ExamAiCountLevel.getByCode(planDTO.getCountLevel());
        if (tendency == null || level == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        String description = planDTO.getDescription().trim();

        AiExamIntentDTO intentRequest = new AiExamIntentDTO();
        intentRequest.setDescription(description);
        AiExamIntentVO intent = aiClient.parseExamIntent(intentRequest);
        int total = level.resolve(intent.getQuestionCount());
        int[] need = distribute(total, tendency.getRatio());
        String query = StrUtil.isNotBlank(intent.getTopics()) ? intent.getTopics() : description;

        // 按难度召回候选池（约为需要题数的 3 倍）
        List<FriendQuestionCandidateVO> pool = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int d = 0; d < DIFFICULTIES.length; d++) {
            if (need[d] == 0) {
                continue;
            }
            FriendQuestionCandidateQueryDTO candidateQuery = new FriendQuestionCandidateQueryDTO();
            candidateQuery.setQuery(query);
            candidateQuery.setDifficulty(DIFFICULTIES[d].getValue());
            candidateQuery.setSize(need[d] * CANDIDATE_FACTOR);
            candidateQuery.setExcludeIds(new ArrayList<>(seen));
            for (FriendQuestionCandidateVO candidate : friendQuestionClient.listCandidates(candidateQuery)) {
                if (seen.add(candidate.getQuestionId())) {
                    pool.add(candidate);
                }
            }
        }
        if (pool.isEmpty()) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS, "题库中没有符合条件的题目");
        }

        List<FriendQuestionCandidateVO> selected = select(description, need, pool);
        ExamAiPlanVO vo = new ExamAiPlanVO();
        vo.setTitle(intent.getTitle());
        vo.setPlannedCount(total);
        vo.setQuestions(selected.stream().map(this::toQuestionVO).toList());
        if (selected.size() < total) {
            vo.setMessage("题库中符合条件的题目不足，计划 " + total + " 道，实际选出 " + selected.size() + " 道");
        }
        log.info("AI 帮建竞赛完成, 计划 = {}, 选出 = {}, 候选 = {}", total, selected.size(), pool.size());
        return vo;
    }

    // 模型挑题后校验：序号必须在候选池内、各难度不超过需要数量；不足的按召回顺序补齐；最终由易到难排列
    private List<FriendQuestionCandidateVO> select(String description, int[] need, List<FriendQuestionCandidateVO> pool) {
        List<Integer> indexes = List.of();
        try {
            indexes = aiClient.selectExamQuestions(toSelectRequest(description, need, pool)).getIndexes();
        } catch (ServiceException e) {
            log.warn("AI 选题失败，改为按检索顺序选题: {}", e.getMessage());
        }
        Map<Integer, List<FriendQuestionCandidateVO>> byDifficulty = new LinkedHashMap<>();
        for (QuestionDifficulty difficulty : DIFFICULTIES) {
            byDifficulty.put(difficulty.getValue(), new ArrayList<>());
        }
        Set<Long> picked = new HashSet<>();
        for (Integer index : indexes) {
            if (index == null || index < 1 || index > pool.size()) {
                continue;
            }
            FriendQuestionCandidateVO candidate = pool.get(index - 1);
            List<FriendQuestionCandidateVO> bucket = byDifficulty.get(candidate.getDifficulty());
            int d = difficultyIndex(candidate.getDifficulty());
            if (bucket != null && d >= 0 && bucket.size() < need[d] && picked.add(candidate.getQuestionId())) {
                bucket.add(candidate);
            }
        }
        for (FriendQuestionCandidateVO candidate : pool) {
            int d = difficultyIndex(candidate.getDifficulty());
            List<FriendQuestionCandidateVO> bucket = byDifficulty.get(candidate.getDifficulty());
            if (bucket != null && d >= 0 && bucket.size() < need[d] && picked.add(candidate.getQuestionId())) {
                bucket.add(candidate);
            }
        }
        List<FriendQuestionCandidateVO> result = new ArrayList<>();
        byDifficulty.values().forEach(result::addAll);
        return result;
    }

    // 组装选题请求（用序号指代候选）
    private AiExamSelectDTO toSelectRequest(String description, int[] need, List<FriendQuestionCandidateVO> pool) {
        AiExamSelectDTO request = new AiExamSelectDTO();
        request.setDescription(description);
        request.setEasyCount(need[0]);
        request.setMediumCount(need[1]);
        request.setHardCount(need[2]);
        List<AiExamCandidateDTO> candidates = new ArrayList<>(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            FriendQuestionCandidateVO source = pool.get(i);
            AiExamCandidateDTO candidate = new AiExamCandidateDTO();
            candidate.setIndex(i + 1);
            candidate.setTitle(source.getTitle());
            candidate.setDifficulty(source.getDifficulty());
            candidate.setSummary(source.getSummary());
            candidate.setPassRate(source.getPassRate());
            candidates.add(candidate);
        }
        request.setCandidates(candidates);
        return request;
    }

    // 按配比把总题数分到三个难度（最大余数法）
    private int[] distribute(int total, int[] ratio) {
        int sum = 0;
        for (int r : ratio) {
            sum += r;
        }
        int[] counts = new int[ratio.length];
        double[] remainders = new double[ratio.length];
        int assigned = 0;
        for (int i = 0; i < ratio.length; i++) {
            double exact = (double) total * ratio[i] / sum;
            counts[i] = (int) Math.floor(exact);
            remainders[i] = exact - counts[i];
            assigned += counts[i];
        }
        List<Integer> order = new ArrayList<>(List.of(0, 1, 2));
        order.sort(Comparator.comparingDouble((Integer i) -> remainders[i]).reversed());
        for (int k = 0; assigned < total; k++) {
            int i = order.get(k % order.size());
            if (ratio[i] > 0) {
                counts[i]++;
                assigned++;
            }
        }
        return counts;
    }

    // 难度值在配比数组中的下标，不合法时返回 -1
    private int difficultyIndex(Integer difficulty) {
        for (int i = 0; i < DIFFICULTIES.length; i++) {
            if (DIFFICULTIES[i].getValue() == (difficulty == null ? -1 : difficulty)) {
                return i;
            }
        }
        return -1;
    }

    // 候选转换为结果题目
    private ExamAiQuestionVO toQuestionVO(FriendQuestionCandidateVO candidate) {
        ExamAiQuestionVO vo = new ExamAiQuestionVO();
        vo.setQuestionId(candidate.getQuestionId());
        vo.setTitle(candidate.getTitle());
        vo.setDifficulty(candidate.getDifficulty());
        vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(candidate.getDifficulty()));
        return vo;
    }
}
