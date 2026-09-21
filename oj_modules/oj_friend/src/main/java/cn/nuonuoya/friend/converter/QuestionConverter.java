package cn.nuonuoya.friend.converter;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.elastic.doc.QuestionDoc;
import cn.nuonuoya.friend.domain.TbQuestion;
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
        vo.setDifficultyDesc(getDifficultyDesc(doc.getDifficulty()));
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

    // 将MySQL实体转换为ES题目文档
    public static QuestionDoc toDoc(TbQuestion entity) {
        if (entity == null) {
            return null;
        }
        QuestionDoc doc = new QuestionDoc();
        doc.setQuestionId(entity.getQuestionId());
        doc.setTitle(entity.getTitle());
        doc.setDifficulty(entity.getDifficulty());
        doc.setTimeLimit(entity.getTimeLimit());
        doc.setSpaceLimit(entity.getSpaceLimit());
        doc.setContent(entity.getContent());
        doc.setQuestionCase(entity.getQuestionCase());
        doc.setDefaultCode(entity.getDefaultCode());
        doc.setMainFunc(entity.getMainFunc());
        doc.setCreateTime(entity.getCreateTime());
        return doc;
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
        vo.setDifficultyDesc(getDifficultyDesc(entity.getDifficulty()));
        vo.setTimeLimit(entity.getTimeLimit());
        vo.setSpaceLimit(entity.getSpaceLimit());
        vo.setContent(entity.getContent());
        vo.setDefaultCode(entity.getDefaultCode());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    // 解析难度描述
    private static String getDifficultyDesc(Integer difficulty) {
        if (difficulty == null) {
            return "未知";
        }
        return switch (difficulty) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "未知";
        };
    }
}
