package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.ai.dto.AiCaseInputDTO;
import cn.nuonuoya.api.ai.dto.AiEditorialDTO;
import cn.nuonuoya.api.ai.vo.AiCaseInputItemVO;
import cn.nuonuoya.api.ai.vo.AiEditorialVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import cn.nuonuoya.api.ai.vo.AiSolutionVO;
import cn.nuonuoya.system.dto.QuestionAiCaseDTO;
import cn.nuonuoya.system.dto.QuestionAiEditorialDTO;
import cn.nuonuoya.system.enums.QuestionCaseType;
import cn.nuonuoya.system.vo.QuestionAiCaseItemVO;
import cn.nuonuoya.system.vo.QuestionAiDraftVO;
import cn.nuonuoya.system.vo.QuestionAiEditorialVO;
import cn.nuonuoya.system.vo.QuestionAiSolutionVO;

import java.util.Map;
import java.util.Objects;

// AI 出题结果转换
public class QuestionAiConverter {

    private QuestionAiConverter() {
    }

    // AI 题面草稿转换为管理端视图（建议标签由名称换成ID，已不存在的名称丢弃）
    public static QuestionAiDraftVO toDraftVO(AiQuestionDraftVO draft, Map<String, Long> tagIdByName) {
        QuestionAiDraftVO vo = BeanUtil.copyProperties(draft, QuestionAiDraftVO.class);
        vo.setTagIds(CollUtil.emptyIfNull(draft.getTags()).stream()
                .map(tagIdByName::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        return vo;
    }

    // 用例生成请求转换为 AI 服务的用例输入请求（标程与时空限制只用于本服务运行标程，不发给 AI）
    public static AiCaseInputDTO toCaseInputRequest(QuestionAiCaseDTO caseDTO) {
        return BeanUtil.copyProperties(caseDTO, AiCaseInputDTO.class);
    }

    // 题解生成请求转换为 AI 服务请求（标题去空白，空的参考解法不传）
    public static AiEditorialDTO toEditorialRequest(QuestionAiEditorialDTO editorialDTO) {
        AiEditorialDTO request = BeanUtil.copyProperties(editorialDTO, AiEditorialDTO.class);
        request.setTitle(editorialDTO.getTitle().trim());
        request.setReferenceCode(StrUtil.trimToNull(editorialDTO.getReferenceCode()));
        return request;
    }

    // AI 题解草稿转换为管理端视图
    public static QuestionAiEditorialVO toEditorialVO(AiEditorialVO editorial) {
        QuestionAiEditorialVO vo = new QuestionAiEditorialVO();
        vo.setContent(editorial.getContent());
        return vo;
    }

    // AI 解法示例转换为管理端视图
    public static QuestionAiSolutionVO toSolutionVO(AiSolutionVO solution) {
        QuestionAiSolutionVO vo = new QuestionAiSolutionVO();
        vo.setCode(solution.getCode());
        return vo;
    }

    // AI 用例输入与标程输出组装为管理端用例（默认隐藏用例，由管理员决定是否公开）
    public static QuestionAiCaseItemVO toCaseItemVO(AiCaseInputItemVO input, String output) {
        QuestionAiCaseItemVO vo = new QuestionAiCaseItemVO();
        vo.setDisplayInput(input.getDisplayInput());
        vo.setDisplayOutput(output);
        vo.setJudgeInput(input.getJudgeInput());
        vo.setJudgeOutput(output);
        vo.setIsSample(QuestionCaseType.HIDDEN.getValue());
        vo.setIntent(input.getIntent());
        return vo;
    }
}
