package cn.nuonuoya.friend.converter;

import cn.nuonuoya.friend.domain.TbQuestionEditorial;
import cn.nuonuoya.friend.vo.QuestionEditorialVO;

// 题目官方题解对象转换器
public class QuestionEditorialConverter {

    private QuestionEditorialConverter() {
    }

    // 题解实体转换为视图（未修改过时以创建时间作为更新时间）
    public static QuestionEditorialVO toVO(TbQuestionEditorial editorial) {
        QuestionEditorialVO vo = new QuestionEditorialVO();
        vo.setContent(editorial.getContent());
        vo.setUpdateTime(editorial.getUpdateTime() != null ? editorial.getUpdateTime() : editorial.getCreateTime());
        return vo;
    }
}
