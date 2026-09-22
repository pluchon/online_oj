package cn.nuonuoya.api.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 文本向量计算请求
@Getter
@Setter
@ToString
public class AiEmbeddingDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 待计算的文本（单次最多 10 条）
    @NotEmpty(message = "文本不能为空")
    @Size(max = 10, message = "单次最多计算10条文本")
    private List<@Size(max = 4000, message = "单条文本不能超过4000个字符") String> texts;

    // 是否为检索查询（查询词与被检索文档使用不同的向量类型）
    private Boolean query;
}
