package cn.nuonuoya.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

// AI 业务配置（模型名与采样参数放 Nacos，可随时切换）
@Getter
@Setter
@ConfigurationProperties(prefix = "oj.ai")
public class AiProperties {

    // 出题类功能（题面草稿、用例输入）使用的模型
    private String questionModel = "qwen3.7-max";

    // 题面草稿的采样温度
    private Double draftTemperature = 0.7;

    // 用例输入的采样温度（略高以覆盖更多情形）
    private Double caseTemperature = 0.8;

    // 做题辅导对话使用的模型
    private String tutorModel = "qwen3.7-flash";

    // 做题辅导的采样温度
    private Double tutorTemperature = 0.5;

    // 做题辅导单次回复的最大 Token
    private Integer tutorMaxTokens = 1500;

    // 做题辅导单次回复的最长耗时（秒）
    private Integer tutorTimeoutSeconds = 90;

    // 文本向量模型
    private String embeddingModel = "text-embedding-v4";

    // 文本向量维度（与题目索引的向量字段一致）
    private Integer embeddingDimensions = 1024;

    // 文本审核模型
    private String moderationModel = "qwen3.7-flash";

    // 竞赛帮建（需求理解与选题）使用的模型
    private String examModel = "qwen3.7-flash";

    // 图片审核模型
    private String imageModerationModel = "qwen3-vl-flash";

    // 只能走 DashScope 多模态接口的模型（纯文本请求也必须走该接口，否则返回 url error）
    private List<String> multimodalModels = new ArrayList<>(List.of("qwen3.7-flash", "qwen3-vl-flash"));

    // 是否开启模型的思考模式（qwen3 系默认开启，会先生成大量推理内容，审核耗时从约 0.6 秒升到 12 秒以上）
    private Boolean enableThinking = false;
}
