package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 题目新增请求参数数据对象（字段见 QuestionBaseDTO）
@Getter
@Setter
@Schema(description = "题目新增请求参数")
public class QuestionAddDTO extends QuestionBaseDTO {
}
