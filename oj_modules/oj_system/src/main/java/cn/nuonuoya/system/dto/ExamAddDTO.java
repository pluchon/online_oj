package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 竞赛新增请求参数DTO（字段见 ExamBaseDTO）
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "竞赛新增请求参数")
public class ExamAddDTO extends ExamBaseDTO {
}
