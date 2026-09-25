package cn.nuonuoya.system.converter;

import cn.nuonuoya.system.domain.TbTag;
import cn.nuonuoya.system.dto.TagSaveDTO;
import cn.nuonuoya.system.enums.TagCategory;
import cn.nuonuoya.system.vo.QuestionTagVO;
import cn.nuonuoya.system.vo.TagVO;

// 标签对象转换器
public class TagConverter {

    private TagConverter() {
    }

    // 保存请求转换为标签实体（名称去掉首尾空白）
    public static TbTag toEntity(TagSaveDTO saveDTO) {
        TbTag tag = new TbTag();
        tag.setTagName(saveDTO.getTagName().trim());
        tag.setCategory(saveDTO.getCategory());
        return tag;
    }

    // 标签实体转换为管理列表视图
    public static TagVO toVO(TbTag tag, int questionCount) {
        TagVO vo = new TagVO();
        vo.setTagId(tag.getTagId());
        vo.setTagName(tag.getTagName());
        vo.setCategory(tag.getCategory());
        vo.setCategoryDesc(TagCategory.getDescByValue(tag.getCategory()));
        vo.setQuestionCount(questionCount);
        return vo;
    }

    // 标签实体转换为题目上的标签视图
    public static QuestionTagVO toQuestionTagVO(TbTag tag) {
        QuestionTagVO vo = new QuestionTagVO();
        vo.setTagId(tag.getTagId());
        vo.setTagName(tag.getTagName());
        return vo;
    }
}
