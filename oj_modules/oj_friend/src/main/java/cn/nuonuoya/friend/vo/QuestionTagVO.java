package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 题目标签视图对象
@Getter
@Setter
@Schema(description = "题目标签")
public class QuestionTagVO {

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
}
