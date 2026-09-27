package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 难题分析中按标签汇总的通过率（计数与通过率见 SubmitStatBaseVO）
@Getter
@Setter
public class OverviewTagStatVO extends SubmitStatBaseVO {

    // 标签名称
    @Schema(description = "标签名称")
    private String tagName;

    // 该标签下参与分析的题数
    @Schema(description = "该标签下参与分析的题数")
    private Integer questionCount = 0;
}
