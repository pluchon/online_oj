package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiQuestionDraftDTO;
import cn.nuonuoya.api.ai.dto.AiSolutionDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputItemVO;
import cn.nuonuoya.api.judge.dto.JudgeCaseDTO;
import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.api.judge.enums.ProgramTypeEnum;
import cn.nuonuoya.api.judge.vo.JudgeCaseResultVO;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.utils.SecurityUtils;
import cn.nuonuoya.system.client.AiClient;
import cn.nuonuoya.system.client.JudgeClient;
import cn.nuonuoya.system.converter.QuestionAiConverter;
import cn.nuonuoya.system.dto.QuestionAiCaseDTO;
import cn.nuonuoya.system.dto.QuestionAiDraftDTO;
import cn.nuonuoya.system.dto.QuestionAiSolutionDTO;
import cn.nuonuoya.system.service.QuestionAiService;
import cn.nuonuoya.system.vo.QuestionAiCaseItemVO;
import cn.nuonuoya.system.vo.QuestionAiCaseVO;
import cn.nuonuoya.system.vo.QuestionAiDraftVO;
import cn.nuonuoya.system.vo.QuestionAiSolutionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// AI 辅助出题实现：预期输出只来自标程在判题沙箱中的实际运行结果，不采用模型计算的答案
@Slf4j
@Service
public class QuestionAiServiceImpl implements QuestionAiService {

    // 判题输出的最大长度（与用例表单限制一致）
    private static final int MAX_OUTPUT_LENGTH = 2000;

    // 编译错误信息回显给管理员的最大长度
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    @Autowired
    private AiClient aiClient;

    @Autowired
    private JudgeClient judgeClient;

    // 生成题面草稿
    @Override
    public QuestionAiDraftVO generateDraft(QuestionAiDraftDTO draftDTO) {
        AiQuestionDraftDTO request = new AiQuestionDraftDTO();
        request.setDescription(draftDTO.getDescription().trim());
        return QuestionAiConverter.toDraftVO(aiClient.generateQuestionDraft(request));
    }

    // 生成测试用例预览：先由模型给出输入，再用标程运行得到输出，运行失败或输出超长的组被丢弃
    @Override
    public QuestionAiCaseVO generateCases(QuestionAiCaseDTO caseDTO) {
        // 未提供标程时先由 AI 生成常见解法作为标程
        if (StrUtil.isBlank(caseDTO.getStandardCode())) {
            caseDTO.setStandardCode(requestSolution(caseDTO.getTitle(), caseDTO.getContent(), caseDTO.getDefaultCode()));
        }
        List<AiCaseInputItemVO> inputs = CollUtil.emptyIfNull(
                aiClient.generateCaseInputs(toCaseInputRequest(caseDTO)).getCases());
        if (inputs.isEmpty()) {
            throw new ServiceException(ResultCode.FAILED_AI_NO_VALID_CASE);
        }

        List<String> outputs = runStandardCode(caseDTO, inputs);
        List<QuestionAiCaseItemVO> cases = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String output = outputs.get(i);
            if (output != null && output.length() <= MAX_OUTPUT_LENGTH) {
                cases.add(QuestionAiConverter.toCaseItemVO(inputs.get(i), output));
            }
        }
        if (cases.isEmpty()) {
            throw new ServiceException(ResultCode.FAILED_AI_NO_VALID_CASE);
        }

        QuestionAiCaseVO vo = new QuestionAiCaseVO();
        vo.setCases(cases);
        vo.setGeneratedCount(inputs.size());
        vo.setStandardCode(caseDTO.getStandardCode());
        vo.setDroppedCount(inputs.size() - cases.size());
        log.info("AI 生成用例完成, 生成 = {}, 可用 = {}", inputs.size(), cases.size());
        return vo;
    }

    // 生成解法示例
    @Override
    public QuestionAiSolutionVO generateSolution(QuestionAiSolutionDTO solutionDTO) {
        QuestionAiSolutionVO vo = new QuestionAiSolutionVO();
        vo.setCode(requestSolution(solutionDTO.getTitle(), solutionDTO.getContent(), solutionDTO.getDefaultCode()));
        return vo;
    }

    // 请求 AI 生成解法代码
    private String requestSolution(String title, String content, String defaultCode) {
        AiSolutionDTO request = new AiSolutionDTO();
        request.setTitle(title);
        request.setContent(content);
        request.setDefaultCode(defaultCode);
        return aiClient.generateSolution(request).getCode();
    }

    // 组装用例输入生成请求
    private AiCaseInputDTO toCaseInputRequest(QuestionAiCaseDTO caseDTO) {
        AiCaseInputDTO request = new AiCaseInputDTO();
        request.setTitle(caseDTO.getTitle());
        request.setContent(caseDTO.getContent());
        request.setDefaultCode(caseDTO.getDefaultCode());
        request.setMainFunc(caseDTO.getMainFunc());
        request.setCount(caseDTO.getCount());
        request.setExistingInputs(caseDTO.getExistingInputs());
        return request;
    }

    // 用标程运行全部输入，返回与输入一一对应的输出（运行失败的位置为 null）；整批失败时逐组重跑以保留可用的组
    private List<String> runStandardCode(QuestionAiCaseDTO caseDTO, List<AiCaseInputItemVO> inputs) {
        JudgeResultVO batch = runOnce(caseDTO, inputs);
        List<String> outputs = collectOutputs(batch, inputs.size());
        if (outputs != null) {
            return outputs;
        }
        log.info("标程整批运行未得到全部输出（{}），改为逐组运行", batch.getStatusDesc());
        List<String> singleOutputs = new ArrayList<>(inputs.size());
        for (AiCaseInputItemVO input : inputs) {
            List<String> single = collectOutputs(runOnce(caseDTO, Collections.singletonList(input)), 1);
            singleOutputs.add(single == null ? null : single.get(0));
        }
        return singleOutputs;
    }

    // 运行一次标程；判题服务不可用或标程编译失败时直接报错
    private JudgeResultVO runOnce(QuestionAiCaseDTO caseDTO, List<AiCaseInputItemVO> inputs) {
        JudgeResultVO result = judgeClient.run(toJudgeRequest(caseDTO, inputs));
        if (result == null || JudgeStatusEnum.SE.getCode().equals(result.getStatus())) {
            throw new ServiceException(ResultCode.ERROR, "判题服务暂不可用，请稍后重试");
        }
        if (JudgeStatusEnum.CE.getCode().equals(result.getStatus())) {
            throw new ServiceException(ResultCode.FAILED_AI_STANDARD_CODE_ERROR,
                    "标程编译失败：" + StrUtil.maxLength(StrUtil.trimToEmpty(result.getExeMessage()), MAX_ERROR_MESSAGE_LENGTH));
        }
        return result;
    }

    // 程序正常结束（通过或答案错误）且每组都有输出时返回全部输出，否则返回 null
    private List<String> collectOutputs(JudgeResultVO result, int expectedSize) {
        boolean exitedNormally = JudgeStatusEnum.AC.getCode().equals(result.getStatus())
                || JudgeStatusEnum.WA.getCode().equals(result.getStatus());
        List<JudgeCaseResultVO> caseResults = result.getCaseResults();
        if (!exitedNormally || caseResults == null || caseResults.size() != expectedSize) {
            return null;
        }
        List<String> outputs = new ArrayList<>(expectedSize);
        for (JudgeCaseResultVO caseResult : caseResults) {
            if (caseResult.getActualOutput() == null) {
                return null;
            }
            outputs.add(caseResult.getActualOutput().trim());
        }
        return outputs;
    }

    // 组装标程运行请求（预期输出留空，只取实际输出）
    private JudgeRequestDTO toJudgeRequest(QuestionAiCaseDTO caseDTO, List<AiCaseInputItemVO> inputs) {
        JudgeRequestDTO requestDTO = new JudgeRequestDTO();
        requestDTO.setUserId(SecurityUtils.getUserId());
        requestDTO.setProgramType(ProgramTypeEnum.JAVA.getCode());
        requestDTO.setUserCode(caseDTO.getStandardCode());
        requestDTO.setMainFunc(caseDTO.getMainFunc());
        requestDTO.setTimeLimit(caseDTO.getTimeLimit());
        requestDTO.setSpaceLimit(caseDTO.getSpaceLimit());
        List<JudgeCaseDTO> cases = new ArrayList<>(inputs.size());
        for (AiCaseInputItemVO input : inputs) {
            JudgeCaseDTO judgeCase = new JudgeCaseDTO();
            judgeCase.setInput(input.getJudgeInput());
            judgeCase.setExpectedOutput("");
            cases.add(judgeCase);
        }
        requestDTO.setCases(cases);
        return requestDTO;
    }
}
