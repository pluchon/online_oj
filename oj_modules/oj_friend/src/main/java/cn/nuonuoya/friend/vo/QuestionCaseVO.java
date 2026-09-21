package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 题目公开示例视图对象
@Getter
@Setter
@Schema(description = "题目公开示例视图对象")
public class QuestionCaseVO {

    // 展示用输入
    @Schema(description = "输入")
    private String input;

    // 展示用输出
    @Schema(description = "输出")
    private String output;
}
