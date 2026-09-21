package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.elastic.doc.QuestionDoc;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.enums.QuestionDifficultyEnum;
import cn.nuonuoya.friend.vo.QuestionVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 题目对象模型转换器
public class QuestionConverter {

    // 将ES题目文档转换为VO
    public static QuestionVO toVO(QuestionDoc doc) {
        if (doc == null) {
            return null;
        }
        QuestionVO vo = new QuestionVO();
        vo.setQuestionId(doc.getQuestionId());
        vo.setTitle(doc.getTitle());
        vo.setDifficulty(doc.getDifficulty());
        vo.setDifficultyDesc(QuestionDifficultyEnum.getDescByCode(doc.getDifficulty()));
        vo.setTimeLimit(doc.getTimeLimit());
        vo.setSpaceLimit(doc.getSpaceLimit());
        vo.setContent(doc.getContent());
        vo.setDefaultCode(doc.getDefaultCode());
        vo.setCreateTime(doc.getCreateTime());
        return vo;
    }

    // 批量将ES题目文档转换为VO列表
    public static List<QuestionVO> toVOListFromDoc(List<QuestionDoc> docList) {
        if (CollUtil.isEmpty(docList)) {
            return Collections.emptyList();
        }
        List<QuestionVO> voList = new ArrayList<>(docList.size());
        for (QuestionDoc doc : docList) {
            voList.add(toVO(doc));
        }
        return voList;
    }

    // 将MySQL实体转换为ES题目文档（字段同名同类型、无派生字段，直接拷贝）
    public static QuestionDoc toDoc(TbQuestion entity) {
        if (entity == null) {
            return null;
        }
        return BeanUtil.copyProperties(entity, QuestionDoc.class);
    }

    // 批量将MySQL实体转换为ES题目文档列表
    public static List<QuestionDoc> toDocList(List<TbQuestion> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return Collections.emptyList();
        }
        List<QuestionDoc> docList = new ArrayList<>(entityList.size());
        for (TbQuestion entity : entityList) {
            docList.add(toDoc(entity));
        }
        return docList;
    }

    // 将MySQL实体转换为VO
    public static QuestionVO toVO(TbQuestion entity) {
        if (entity == null) {
            return null;
        }
        QuestionVO vo = new QuestionVO();
        vo.setQuestionId(entity.getQuestionId());
        vo.setTitle(entity.getTitle());
        vo.setDifficulty(entity.getDifficulty());
        vo.setDifficultyDesc(QuestionDifficultyEnum.getDescByCode(entity.getDifficulty()));
        vo.setTimeLimit(entity.getTimeLimit());
        vo.setSpaceLimit(entity.getSpaceLimit());
        vo.setContent(entity.getContent());
        vo.setDefaultCode(entity.getDefaultCode());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
