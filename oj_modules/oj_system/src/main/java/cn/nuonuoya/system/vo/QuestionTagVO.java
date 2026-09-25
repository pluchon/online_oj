package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 题目上的标签视图对象
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
}
