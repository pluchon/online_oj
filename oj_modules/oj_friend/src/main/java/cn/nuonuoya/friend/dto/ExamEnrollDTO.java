package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 竞赛报名请求参数
@Getter
@Setter
@ToString
@Schema(description = "竞赛报名请求参数")
public class ExamEnrollDTO {

    // 目标竞赛ID
    @NotNull(message = "竞赛ID不能为空")
    @Schema(description = "竞赛ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long examId;
}
