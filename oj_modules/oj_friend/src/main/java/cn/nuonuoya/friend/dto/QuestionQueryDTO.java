package cn.nuonuoya.friend.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目列表分页与全文检索DTO（每页条数默认值与纠正规则沿用 PageQuery）
@Getter
@Setter
@ToString
@Schema(description = "题目列表分页与全文检索参数")
public class QuestionQueryDTO extends PageQuery {

    // 搜索关键字（按标题或内容模糊匹配）
    @Schema(description = "搜索关键字（按标题或内容模糊匹配）")
    private String keyword;

    // 题目难度（1:简单 2:中等 3:困难，为空表示全部）
    @Schema(description = "题目难度（1:简单 2:中等 3:困难，为空表示全部）")
    private Integer difficulty;

    // 标签分类（1:数据结构 2:算法 3:数学 4:其他，只看带该分类下任一标签的题目，为空表示全部）
    @Schema(description = "标签分类（1:数据结构 2:算法 3:数学 4:其他，为空表示全部）")
    private Integer tagCategory;

    // 标签ID（为空表示全部）
    @Schema(description = "标签ID（为空表示全部）")
    private Long tagId;

    // 做题状态（0:未尝试 1:已攻克 2:尝试中，为空或未登录时不筛选）
    @Schema(description = "做题状态（0:未尝试 1:已攻克 2:尝试中，为空或未登录时不筛选）")
    private Integer userStatus;
}
