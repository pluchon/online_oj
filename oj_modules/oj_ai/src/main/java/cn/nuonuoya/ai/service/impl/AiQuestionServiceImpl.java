package cn.nuonuoya.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.ai.config.AiChatOptionsFactory;
import cn.nuonuoya.ai.config.AiProperties;
import cn.nuonuoya.ai.exception.AiModelException;
import cn.nuonuoya.ai.prompt.QuestionPrompts;
import cn.nuonuoya.ai.service.AiQuestionService;
import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiQuestionDraftDTO;
import cn.nuonuoya.api.ai.dto.AiSolutionDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputItemVO;
import cn.nuonuoya.api.ai.vo.AiCaseInputVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import cn.nuonuoya.api.ai.vo.AiSolutionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 出题类 AI 能力实现：模型只给题面与输入，结果做范围兜底，不计算预期输出
@Slf4j
@Service
public class AiQuestionServiceImpl implements AiQuestionService {

    // 单组判题输入的最大长度（与题目用例表单限制一致）
    private static final int MAX_JUDGE_INPUT_LENGTH = 10000;

    // 单组展示输入的最大长度
    private static final int MAX_DISPLAY_INPUT_LENGTH = 2000;

    // 未指定数量时单次最多保留的用例组数
    private static final int AUTO_CASE_LIMIT = 5;

    // 解法示例使用的采样温度（偏向稳定的主流解法）
    private static final double SOLUTION_TEMPERATURE = 0.2;

    // 空间限制的上限（判题容器内存为 256MB，需给 JVM 自身留余量）
    private static final int MAX_SPACE_LIMIT_MB = 200;

    @Autowired
    private ChatClient questionChatClient;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private AiChatOptionsFactory aiChatOptionsFactory;

    // 生成题面草稿，并把难度与时空限制收敛到合法范围
    @Override
    public AiQuestionDraftVO generateQuestionDraft(AiQuestionDraftDTO draftDTO) {
        AiQuestionDraftVO draft = callForEntity("题面草稿", QuestionPrompts.DRAFT_SYSTEM,
                QuestionPrompts.draftUser(draftDTO.getDescription().trim()),
                aiProperties.getDraftTemperature(), AiQuestionDraftVO.class);
        if (StrUtil.isBlank(draft.getTitle()) || StrUtil.isBlank(draft.getContent())) {
            throw new AiModelException("题面草稿缺少标题或描述");
        }
        draft.setTitle(draft.getTitle().trim());
        draft.setContent(draft.getContent().trim());
        draft.setDefaultCode(StrUtil.trimToEmpty(draft.getDefaultCode()));
        draft.setMainFunc(StrUtil.trimToEmpty(draft.getMainFunc()));
        draft.setDifficulty(clamp(draft.getDifficulty(), 1, 3, 2));
        draft.setTimeLimit(clamp(draft.getTimeLimit(), 500, 5000, 1000));
        draft.setSpaceLimit(clamp(draft.getSpaceLimit(), 32, MAX_SPACE_LIMIT_MB, 128));
        return draft;
    }

    // 生成用例输入，剔除空输入、超长输入以及与已有用例或彼此重复的输入
    @Override
    public AiCaseInputVO generateCaseInputs(AiCaseInputDTO caseInputDTO) {
        List<String> existing = CollUtil.emptyIfNull(caseInputDTO.getExistingInputs());
        String existingText = existing.isEmpty() ? "（无）" : String.join("\n---\n", existing);
        AiCaseInputVO generated = callForEntity("用例输入", QuestionPrompts.CASE_SYSTEM,
                QuestionPrompts.caseUser(caseInputDTO.getTitle(), caseInputDTO.getContent(),
                        caseInputDTO.getDefaultCode(), caseInputDTO.getMainFunc(),
                        caseInputDTO.getCount(), existingText),
                aiProperties.getCaseTemperature(), AiCaseInputVO.class);

        int limit = caseInputDTO.getCount() == null ? AUTO_CASE_LIMIT : caseInputDTO.getCount();
        Set<String> seen = new HashSet<>();
        existing.forEach(input -> seen.add(normalize(input)));
        List<AiCaseInputItemVO> accepted = new ArrayList<>();
        for (AiCaseInputItemVO item : CollUtil.emptyIfNull(generated.getCases())) {
            if (item == null || StrUtil.isBlank(item.getDisplayInput()) || item.getJudgeInput() == null) {
                continue;
            }
            String judgeInput = normalize(item.getJudgeInput());
            if (judgeInput.length() > MAX_JUDGE_INPUT_LENGTH || item.getDisplayInput().length() > MAX_DISPLAY_INPUT_LENGTH
                    || !seen.add(judgeInput)) {
                continue;
            }
            item.setJudgeInput(judgeInput);
            item.setDisplayInput(item.getDisplayInput().trim());
            item.setIntent(StrUtil.trimToEmpty(item.getIntent()));
            accepted.add(item);
            if (accepted.size() >= limit) {
                break;
            }
        }
        AiCaseInputVO result = new AiCaseInputVO();
        result.setCases(accepted);
        return result;
    }

    // 生成常见解法，缺少代码时视为失败
    @Override
    public AiSolutionVO generateSolution(AiSolutionDTO solutionDTO) {
        AiSolutionVO solution = callForEntity("解法示例", QuestionPrompts.SOLUTION_SYSTEM,
                QuestionPrompts.solutionUser(solutionDTO.getTitle(), solutionDTO.getContent(), solutionDTO.getDefaultCode()),
                SOLUTION_TEMPERATURE, AiSolutionVO.class);
        if (StrUtil.isBlank(solution.getCode())) {
            throw new AiModelException("解法示例缺少代码");
        }
        solution.setCode(solution.getCode().trim());
        return solution;
    }

    // 以指定模型与温度调用模型并把回复解析为结构化对象，任何失败都转换为模型调用异常
    private <T> T callForEntity(String scene, String system, String user, Double temperature, Class<T> type) {
        long start = System.currentTimeMillis();
        String model = aiProperties.getQuestionModel();
        try {
            T entity = questionChatClient.prompt()
                    .options(aiChatOptionsFactory.builder(model, temperature).build())
                    .system(system)
                    .user(user)
                    .call()
                    .entity(type);
            if (entity == null) {
                throw new AiModelException(scene + "：模型返回为空");
            }
            log.info("AI {}完成, model = {}, 耗时 = {} ms", scene, model, System.currentTimeMillis() - start);
            return entity;
        } catch (AiModelException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI {}失败, model = {}, 耗时 = {} ms, error = {}", scene, model,
                    System.currentTimeMillis() - start, e.getMessage());
            throw new AiModelException(scene + "：模型调用失败", e);
        }
    }

    // 统一换行并去掉首尾空行，保留行内空格（空数组以空行表示）
    private String normalize(String input) {
        String text = input.replace("\r", "");
        while (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        while (text.startsWith("\n")) {
            text = text.substring(1);
        }
        return text;
    }

    // 把数值收敛到区间内，缺失时使用默认值
    private Integer clamp(Integer value, int min, int max, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, value));
    }
}
