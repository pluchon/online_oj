package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 标签管理列表项视图对象
@Getter
@Setter
@Schema(description = "标签")
public class TagVO {

    // 标签ID（序列化为字符串防止前端精度丢失）
    @Schema(description = "标签ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tagId;

    // 标签名称
    @Schema(description = "标签名称")
    private String tagName;

    // 标签分类（1:数据结构 2:算法 3:数学 4:其他）
    @Schema(description = "标签分类（1:数据结构 2:算法 3:数学 4:其他）")
    private Integer category;

    // 标签分类描述
    @Schema(description = "标签分类描述")
    private String categoryDesc;

    // 使用该标签的题目数
    @Schema(description = "使用该标签的题目数")
    private Integer questionCount;
}
