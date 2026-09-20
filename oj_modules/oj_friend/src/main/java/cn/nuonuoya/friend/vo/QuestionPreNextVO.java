package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 上一题下一题导航视图对象
@Getter
@Setter
@Schema(description = "上一题下一题导航视图对象")
public class QuestionPreNextVO {

    // 上一题ID（序列化为字符串防止JS前端丢失精度）
    @Schema(description = "上一题ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long preQuestionId;

    // 下一题ID（序列化为字符串防止JS前端丢失精度）
    @Schema(description = "下一题ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long nextQuestionId;
}
