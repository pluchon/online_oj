package cn.nuonuoya.system.converter;

import cn.nuonuoya.api.ai.vo.AiCaseInputItemVO;
import cn.nuonuoya.api.ai.vo.AiQuestionDraftVO;
import cn.nuonuoya.api.ai.vo.AiSolutionVO;
import cn.nuonuoya.system.enums.QuestionCaseType;
import cn.nuonuoya.system.vo.QuestionAiCaseItemVO;
import cn.nuonuoya.system.vo.QuestionAiDraftVO;
import cn.nuonuoya.system.vo.QuestionAiSolutionVO;

// AI 出题结果转换
public class QuestionAiConverter {

    private QuestionAiConverter() {
    }

    // AI 题面草稿转换为管理端视图
    public static QuestionAiDraftVO toDraftVO(AiQuestionDraftVO draft) {
        QuestionAiDraftVO vo = new QuestionAiDraftVO();
        vo.setTitle(draft.getTitle());
        vo.setDifficulty(draft.getDifficulty());
        vo.setTimeLimit(draft.getTimeLimit());
        vo.setSpaceLimit(draft.getSpaceLimit());
        vo.setContent(draft.getContent());
        vo.setDefaultCode(draft.getDefaultCode());
        vo.setMainFunc(draft.getMainFunc());
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
