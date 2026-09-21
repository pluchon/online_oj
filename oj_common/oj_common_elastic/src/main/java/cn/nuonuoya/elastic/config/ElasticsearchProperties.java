package cn.nuonuoya.elastic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Elasticsearch 连接配置
@Data
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {

    // 节点连接地址
    private String uris = "http://localhost:9200";

    // 建立连接超时时间（毫秒）
    private Integer connectTimeout = 1000;

    // 读取超时时间（毫秒）
    private Integer socketTimeout = 30000;
}
