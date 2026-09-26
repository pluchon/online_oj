package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.domain.TbQuestionCase;
import cn.nuonuoya.system.dto.QuestionBaseDTO;
import cn.nuonuoya.system.dto.QuestionCaseDTO;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.enums.QuestionPurpose;
import cn.nuonuoya.system.vo.QuestionCaseVO;
import cn.nuonuoya.system.vo.QuestionDetailVO;

import java.util.ArrayList;
import java.util.List;

// 题目对象转换器
public class QuestionConverter {

    // 新增或修改请求转换为题目实体（修改请求会带上主键；用例、标签、题解由调用方另存）
    public static TbQuestion toEntity(QuestionBaseDTO dto) {
        return BeanUtil.copyProperties(dto, TbQuestion.class);
    }

    // 用例请求列表转换为实体列表（排序号按提交顺序生成）
    public static List<TbQuestionCase> toCaseEntities(Long questionId, List<QuestionCaseDTO> cases) {
        List<TbQuestionCase> entities = new ArrayList<>(cases.size());
        for (int i = 0; i < cases.size(); i++) {
            QuestionCaseDTO dto = cases.get(i);
            TbQuestionCase entity = new TbQuestionCase();
            entity.setQuestionId(questionId);
            entity.setDisplayInput(dto.getDisplayInput().trim());
            entity.setDisplayOutput(dto.getDisplayOutput().trim());
            entity.setJudgeInput(dto.getJudgeInput().replace("\r", "").strip());
            entity.setJudgeOutput(dto.getJudgeOutput().trim());
            entity.setIsSample(dto.getIsSample());
            entity.setSortOrder(i + 1);
            entities.add(entity);
        }
        return entities;
    }

    // 用例实体列表转换为视图列表
    public static List<QuestionCaseVO> toCaseVOList(List<TbQuestionCase> cases) {
        return BeanUtil.copyToList(cases, QuestionCaseVO.class);
    }

    // 题目实体转换为详情视图对象（用例、标签、题解由调用方补充）
    public static QuestionDetailVO toDetailVO(TbQuestion question) {
        if (question == null) {
            return null;
        }
        QuestionDetailVO vo = BeanUtil.copyProperties(question, QuestionDetailVO.class);
        vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(question.getDifficulty()));
        vo.setPurposeDesc(QuestionPurpose.getDescByValue(question.getPurpose()));
        // 代码块防空处理，避免前端代码编辑器因 null 抛出异常
        vo.setDefaultCode(StrUtil.nullToEmpty(question.getDefaultCode()));
        vo.setMainFunc(StrUtil.nullToEmpty(question.getMainFunc()));
        return vo;
    }
}
