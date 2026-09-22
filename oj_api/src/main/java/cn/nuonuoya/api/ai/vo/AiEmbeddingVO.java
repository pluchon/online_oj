package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 文本向量计算结果
@Getter
@Setter
@ToString(exclude = "vectors")
public class AiEmbeddingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 使用的模型（模型变化时调用方需重算全部向量）
    private String model;

    // 向量维度
    private Integer dimensions;

    // 与输入顺序一致的向量
    private List<float[]> vectors;
}
