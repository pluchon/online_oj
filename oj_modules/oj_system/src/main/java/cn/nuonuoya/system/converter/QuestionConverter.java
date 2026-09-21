package cn.nuonuoya.system.converter;

import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.dto.QuestionAddDTO;
import cn.nuonuoya.system.dto.QuestionEditDTO;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.vo.QuestionDetailVO;

// 题目对象转换器
public class QuestionConverter {

    // 新增请求转换为题目实体
    public static TbQuestion toEntity(QuestionAddDTO addDTO) {
        if (addDTO == null) {
            return null;
        }
        TbQuestion question = new TbQuestion();
        fillEditableFields(question, addDTO);
        return question;
    }

    // 修改请求转换为题目实体（仅携带主键与可编辑字段）
    public static TbQuestion toEntity(QuestionEditDTO editDTO) {
        if (editDTO == null) {
            return null;
        }
        TbQuestion question = new TbQuestion();
        question.setQuestionId(editDTO.getQuestionId());
        fillEditableFields(question, editDTO);
        return question;
    }

    // 题目实体转换为详情视图对象
    public static QuestionDetailVO toDetailVO(TbQuestion question) {
        if (question == null) {
            return null;
        }
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setQuestionId(question.getQuestionId());
        vo.setTitle(question.getTitle());
        vo.setDifficulty(question.getDifficulty());
        vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(question.getDifficulty()));
        vo.setTimeLimit(question.getTimeLimit());
        vo.setSpaceLimit(question.getSpaceLimit());
        vo.setContent(question.getContent());
        vo.setQuestionCase(question.getQuestionCase());
        // 代码块防空处理，避免前端代码编辑器因 null 抛出异常
        vo.setDefaultCode(question.getDefaultCode() == null ? "" : question.getDefaultCode());
        vo.setMainFunc(question.getMainFunc() == null ? "" : question.getMainFunc());
        vo.setCreateTime(question.getCreateTime());
        return vo;
    }

    // 填充新增与修改共用的可编辑字段
    private static void fillEditableFields(TbQuestion question, QuestionAddDTO dto) {
        question.setTitle(dto.getTitle());
        question.setDifficulty(dto.getDifficulty());
        question.setTimeLimit(dto.getTimeLimit());
        question.setSpaceLimit(dto.getSpaceLimit());
        question.setContent(dto.getContent());
        question.setQuestionCase(dto.getQuestionCase());
        question.setDefaultCode(dto.getDefaultCode());
        question.setMainFunc(dto.getMainFunc());
    }
}
