package cn.nuonuoya.elastic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Elasticsearch连接配置属性类
@Data
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {

    // ES集群节点连接地址
    private String uris = "http://localhost:9200";

    // 建立连接超时时间（毫秒）
    private Integer connectTimeout = 1000;

    // 数据读取套接字超时时间（毫秒）
    private Integer socketTimeout = 30000;

    // 从连接池获取连接的等待超时时间（毫秒）
    private Integer connectionRequestTimeout = 500;

    // 连接池支持的最大总连接数
    private Integer maxConnTotal = 100;

    public String getUris() {
        return uris;
    }

    public void setUris(String uris) {
        this.uris = uris;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public Integer getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(Integer connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public Integer getMaxConnTotal() {
        return maxConnTotal;
    }

    public void setMaxConnTotal(Integer maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
    }
}
