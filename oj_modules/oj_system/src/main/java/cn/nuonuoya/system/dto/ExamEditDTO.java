package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 竞赛编辑请求参数DTO（公共字段见 ExamBaseDTO，另带竞赛ID）
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "竞赛编辑请求参数")
public class ExamEditDTO extends ExamBaseDTO {

    // 竞赛ID
    @Schema(description = "竞赛ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long examId;
}
