package cn.nuonuoya.friend.converter;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.ai.dto.AiTutorChatDTO;
import cn.nuonuoya.api.ai.dto.AiTutorHistoryDTO;
import cn.nuonuoya.api.ai.dto.AiTutorSampleDTO;
import cn.nuonuoya.api.ai.dto.AiTutorSubmissionDTO;
import cn.nuonuoya.api.ai.enums.AiTutorActionEnum;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.friend.domain.TbAiChatMessage;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbQuestionCase;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.enums.AiChatRoleEnum;
import cn.nuonuoya.friend.enums.QuestionCaseTypeEnum;
import cn.nuonuoya.friend.vo.AiTutorMessageVO;

import java.util.List;
import java.util.Objects;

// AI 辅导相关转换：消息视图、发给 AI 服务的上下文
public class AiTutorConverter {

    // 发送给模型的公开示例上限
    private static final int SAMPLE_LIMIT = 5;

    // 发送给模型的编译或运行信息最大长度
    private static final int EXE_MESSAGE_LIMIT = 2000;

    private AiTutorConverter() {
    }

    // 消息实体转视图
    public static AiTutorMessageVO toMessageVO(TbAiChatMessage message) {
        AiTutorMessageVO vo = new AiTutorMessageVO();
        vo.setMessageId(message.getMessageId());
        vo.setFromUser(AiChatRoleEnum.USER.getCode().equals(message.getRole()));
        vo.setAction(message.getAction());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    // 批量转换消息视图
    public static List<AiTutorMessageVO> toMessageVOList(List<TbAiChatMessage> messages) {
        return messages.stream().map(AiTutorConverter::toMessageVO).toList();
    }

    // 消息实体转历史对话
    public static List<AiTutorHistoryDTO> toHistoryList(List<TbAiChatMessage> messages) {
        return messages.stream().map(message -> {
            AiTutorHistoryDTO item = new AiTutorHistoryDTO();
            item.setFromUser(AiChatRoleEnum.USER.getCode().equals(message.getRole()));
            item.setContent(message.getContent());
            return item;
        }).toList();
    }

    // 组装发给 AI 服务的上下文（题面、公开示例、当前代码、可选的被分析提交）
    public static AiTutorChatDTO toChatDTO(TbQuestion question, List<TbQuestionCase> allCases, AiTutorActionEnum action,
                                           String content, String userCode, TbUserSubmit submit) {
        AiTutorChatDTO dto = new AiTutorChatDTO();
        dto.setAction(action.getCode());
        dto.setQuestionTitle(question.getTitle());
        dto.setQuestionContent(question.getContent());
        dto.setDefaultCode(question.getDefaultCode());
        dto.setUserCode(StrUtil.isBlank(userCode) ? null : userCode);
        dto.setMessage(StrUtil.isBlank(content) ? null : content);
        dto.setSamples(allCases.stream()
                .filter(c -> QuestionCaseTypeEnum.SAMPLE.getCode().equals(c.getIsSample()))
                .limit(SAMPLE_LIMIT)
                .map(c -> {
                    AiTutorSampleDTO sample = new AiTutorSampleDTO();
                    sample.setInput(c.getDisplayInput());
                    sample.setOutput(c.getDisplayOutput());
                    return sample;
                })
                .toList());
        if (submit != null) {
            dto.setSubmission(toSubmissionDTO(submit, allCases));
        }
        return dto;
    }

    // 提交记录转换为 AI 上下文：隐藏用例只给序号，不给输入与预期输出
    private static AiTutorSubmissionDTO toSubmissionDTO(TbUserSubmit submit, List<TbQuestionCase> allCases) {
        AiTutorSubmissionDTO dto = new AiTutorSubmissionDTO();
        dto.setCode(submit.getUserCode());
        JudgeStatusEnum status = JudgeStatusEnum.getByCode(submit.getJudgeStatus());
        dto.setVerdict(status == null ? null : status.getDesc());
        dto.setPassCount(submit.getPassCount());
        dto.setTotalCount(submit.getTotalCount());
        dto.setExeMessage(StrUtil.maxLength(submit.getExeMessage(), EXE_MESSAGE_LIMIT));
        if (submit.getFailCaseId() == null) {
            return dto;
        }
        for (int i = 0; i < allCases.size(); i++) {
            TbQuestionCase c = allCases.get(i);
            if (!Objects.equals(c.getCaseId(), submit.getFailCaseId())) {
                continue;
            }
            dto.setFailCaseIndex(i + 1);
            boolean sample = QuestionCaseTypeEnum.SAMPLE.getCode().equals(c.getIsSample());
            dto.setFailCaseSample(sample);
            if (sample) {
                dto.setFailInput(c.getDisplayInput());
                dto.setFailExpected(c.getDisplayOutput());
                dto.setFailActual(submit.getFailOutput());
            }
            break;
        }
        return dto;
    }
}
