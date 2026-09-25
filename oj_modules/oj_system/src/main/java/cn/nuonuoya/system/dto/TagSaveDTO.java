package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 标签新增与修改请求参数
@Getter
@Setter
@Schema(description = "标签新增与修改请求参数")
public class TagSaveDTO {

    // 标签名称
    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 20, message = "标签名称不能超过20个字符")
    private String tagName;

    // 标签分类（1:数据结构 2:算法 3:数学 4:其他）
    @Schema(description = "标签分类（1:数据结构 2:算法 3:数学 4:其他）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "标签分类不能为空")
    @Min(value = 1, message = "标签分类不合法")
    @Max(value = 4, message = "标签分类不合法")
    private Integer category;
}
