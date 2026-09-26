package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.nuonuoya.api.friend.vo.FriendQuestionCandidateVO;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.dto.ExamBaseDTO;
import cn.nuonuoya.system.enums.ExamStatus;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.vo.ExamAiQuestionVO;
import cn.nuonuoya.system.vo.ExamDetailVO;

// 竞赛对象转换器
public class ExamConverter {

    // 新增或修改请求转换为竞赛实体（名称去掉首尾空白；修改请求会带上主键）
    public static TbExam toEntity(ExamBaseDTO dto) {
        TbExam exam = BeanUtil.copyProperties(dto, TbExam.class);
        exam.setTitle(dto.getTitle().trim());
        return exam;
    }

    // 实体对象转换为详情VO对象
    public static ExamDetailVO toDetailVO(TbExam entity, String creatorName) {
        if (entity == null) {
            return null;
        }
        ExamDetailVO vo = BeanUtil.copyProperties(entity, ExamDetailVO.class);
        vo.setStatusDesc(ExamStatus.getDescByValue(entity.getStatus()));
        vo.setCreatorName(creatorName);
        return vo;
    }

    // AI 帮建竞赛选出的候选题转换为结果题目
    public static ExamAiQuestionVO toAiQuestionVO(FriendQuestionCandidateVO candidate) {
        ExamAiQuestionVO vo = BeanUtil.copyProperties(candidate, ExamAiQuestionVO.class);
        vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(candidate.getDifficulty()));
        return vo;
    }
}
