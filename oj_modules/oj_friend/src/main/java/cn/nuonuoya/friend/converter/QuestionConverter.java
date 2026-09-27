package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.elastic.doc.QuestionDoc;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.enums.QuestionDifficultyEnum;
import cn.nuonuoya.friend.vo.QuestionVO;

import java.util.Collections;
import java.util.List;

// 题目对象模型转换器
public class QuestionConverter {

    // 将ES题目文档转换为VO
    public static QuestionVO toVO(QuestionDoc doc) {
        return doc == null ? null : toVO(doc, doc.getDifficulty());
    }

    // 将MySQL实体转换为VO
    public static QuestionVO toVO(TbQuestion entity) {
        return entity == null ? null : toVO(entity, entity.getDifficulty());
    }

    // 批量将ES题目文档转换为VO列表
    public static List<QuestionVO> toVOListFromDoc(List<QuestionDoc> docList) {
        return CollUtil.isEmpty(docList) ? Collections.emptyList() : docList.stream().map(QuestionConverter::toVO).toList();
    }

    // 批量将MySQL实体转换为ES题目文档列表（字段同名同类型、无派生字段，直接拷贝）
    public static List<QuestionDoc> toDocList(List<TbQuestion> entityList) {
        return CollUtil.isEmpty(entityList) ? Collections.emptyList() : BeanUtil.copyToList(entityList, QuestionDoc.class);
    }

    // ES文档与MySQL实体共用的VO转换：同名字段直接拷贝（main函数等不在VO里的字段不会带出），补难度描述
    private static QuestionVO toVO(Object source, Integer difficulty) {
        QuestionVO vo = BeanUtil.copyProperties(source, QuestionVO.class);
        vo.setDifficultyDesc(QuestionDifficultyEnum.getDescByCode(difficulty));
        return vo;
    }
}
