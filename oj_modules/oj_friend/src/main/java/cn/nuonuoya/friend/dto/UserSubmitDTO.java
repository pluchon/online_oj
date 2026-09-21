package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 用户提交代码请求DTO
@Getter
@Setter
@Schema(description = "用户提交代码请求DTO")
public class UserSubmitDTO {

    // 题目ID（由路径参数填充）
    @Schema(hidden = true)
    private Long questionId;

    // 竞赛ID (为空表示非竞赛日常刷题)
    @Schema(description = "竞赛ID")
    private Long examId;

    // 用户编写的代码
    @NotBlank(message = "代码内容不能为空")
    @Schema(description = "用户编写的代码")
    private String userCode;
}
