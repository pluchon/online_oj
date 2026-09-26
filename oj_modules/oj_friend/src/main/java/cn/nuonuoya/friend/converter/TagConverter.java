package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.nuonuoya.friend.domain.TbTag;
import cn.nuonuoya.friend.enums.TagCategoryEnum;
import cn.nuonuoya.friend.vo.QuestionTagVO;

// 题目标签对象转换器
public class TagConverter {

    private TagConverter() {
    }

    // 标签实体转换为视图
    public static QuestionTagVO toVO(TbTag tag) {
        QuestionTagVO vo = BeanUtil.copyProperties(tag, QuestionTagVO.class);
        vo.setCategoryDesc(TagCategoryEnum.getDescByCode(tag.getCategory()));
        return vo;
    }
}
