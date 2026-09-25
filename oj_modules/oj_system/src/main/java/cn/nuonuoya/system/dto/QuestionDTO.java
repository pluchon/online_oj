package cn.nuonuoya.system.dto;

import cn.nuonuoya.common.domain.PageQuery;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目列表分页查询DTO
@Getter
@Setter
@ToString
public class QuestionDTO extends PageQuery {

    // 题目难度（1:简单 2:中等 3:困难）
    private Integer difficulty;

    // 题目标题（支持模糊查询）
    private String title;

    // 标签分类（只看带该分类下任一标签的题目）
    private Integer tagCategory;

    // 标签ID（只看带该标签的题目）
    private Long tagId;
}
