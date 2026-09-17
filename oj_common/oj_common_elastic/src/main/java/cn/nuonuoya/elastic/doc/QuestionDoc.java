package cn.nuonuoya.elastic.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

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

    // 题目测试用例
    @Field(type = FieldType.Keyword, index = false)
    private String questionCase;

    // 默认代码模板
    @Field(type = FieldType.Keyword, index = false)
    private String defaultCode;

    // 评测主函数
    @Field(type = FieldType.Keyword, index = false)
    private String mainFunc;

    // 创建时间
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Integer getSpaceLimit() {
        return spaceLimit;
    }

    public void setSpaceLimit(Integer spaceLimit) {
        this.spaceLimit = spaceLimit;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getQuestionCase() {
        return questionCase;
    }

    public void setQuestionCase(String questionCase) {
        this.questionCase = questionCase;
    }

    public String getDefaultCode() {
        return defaultCode;
    }

    public void setDefaultCode(String defaultCode) {
        this.defaultCode = defaultCode;
    }

    public String getMainFunc() {
        return mainFunc;
    }

    public void setMainFunc(String mainFunc) {
        this.mainFunc = mainFunc;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
