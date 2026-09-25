package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 题目官方题解视图对象
@Getter
@Setter
@Schema(description = "题目官方题解")
public class QuestionEditorialVO {

    // 题解内容（Markdown）
    @Schema(description = "题解内容（Markdown）")
    private String content;

    // 最近更新时间（未修改过时为创建时间）
    @Schema(description = "最近更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
