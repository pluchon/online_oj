package cn.nuonuoya.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 竞赛编辑请求参数DTO
@Getter
@Setter
@ToString
@Schema(description = "竞赛编辑请求参数")
public class ExamEditDTO {

    // 竞赛ID
    @Schema(description = "竞赛ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long examId;

    // 竞赛名称
    @Schema(description = "竞赛名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "竞赛名称不能为空")
    @Size(max = 30, message = "竞赛名称长度不能超过30个字符")
    private String title;

    // 竞赛开始时间
    @Schema(description = "竞赛开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "竞赛开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    // 竞赛结束时间
    @Schema(description = "竞赛结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "竞赛结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
