package cn.nuonuoya.elastic.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.KnnSimilarity;

import java.time.LocalDateTime;
import java.util.List;

// 题目ES文档映射实体
@Data
@Document(indexName = "question")
public class QuestionDoc {

    // 题目主键ID
    @Id
    private Long questionId;

    // 题目标题（采用最少切分ik_smart）
    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String title;

    // 题目难度（1:简单 2:中等 3:困难）
    @Field(type = FieldType.Integer)
    private Integer difficulty;

    // 时间限制（毫秒）
    @Field(type = FieldType.Integer)
    private Integer timeLimit;

    // 空间限制（MB）
    @Field(type = FieldType.Integer)
    private Integer spaceLimit;

    // 题目内容描述（采用最少切分ik_smart）
    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String content;

    // 默认代码模板
    @Field(type = FieldType.Keyword, index = false)
    private String defaultCode;

    // 评测主函数
    @Field(type = FieldType.Keyword, index = false)
    private String mainFunc;

    // 题目向量（标题与描述的文本向量，用于语义检索与相似题推荐）
    @Field(type = FieldType.Dense_Vector, dims = 1024, knnSimilarity = KnnSimilarity.COSINE)
    private float[] embedding;

    // 生成向量时文本的摘要（文本未变化时复用已有向量）
    @Field(type = FieldType.Keyword, index = false)
    private String embeddingHash;

    // 创建时间
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    // 标签ID列表（题库按标签筛选）
    @Field(type = FieldType.Long)
    private List<Long> tagIds;
}
