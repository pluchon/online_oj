package cn.nuonuoya.elastic.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.Duration;

// Elasticsearch统一自动装配配置类
@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
@EnableElasticsearchRepositories(basePackages = "cn.nuonuoya.elastic.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    // 注入ES连接配置
    @Autowired
    private ElasticsearchProperties properties;

    // 构造ES客户端基础配置
    @Override
    public ClientConfiguration clientConfiguration() {
        String uri = properties.getUris();
        if (uri != null) {
            uri = uri.replace("http://", "").replace("https://", "");
        } else {
            uri = "127.0.0.1:9200";
        }
        return ClientConfiguration.builder()
                .connectedTo(uri)
                .withConnectTimeout(Duration.ofMillis(properties.getConnectTimeout() != null ? properties.getConnectTimeout() : 1000))
                .withSocketTimeout(Duration.ofMillis(properties.getSocketTimeout() != null ? properties.getSocketTimeout() : 30000))
                .build();
    }
}
