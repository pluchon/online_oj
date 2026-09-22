package cn.nuonuoya.friend.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// 调用 AI 流式接口的 WebClient（按服务名负载均衡）
@Configuration
public class AiWebClientConfig {

    // 支持 http://oj-ai 形式地址的 WebClient 构建器
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
