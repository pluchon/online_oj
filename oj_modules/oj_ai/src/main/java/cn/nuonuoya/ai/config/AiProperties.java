package cn.nuonuoya.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
}
