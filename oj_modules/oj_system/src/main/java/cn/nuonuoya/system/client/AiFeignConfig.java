package cn.nuonuoya.system.client;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

// AI 服务 Feign 客户端专用配置（不加 @Configuration，只作用于 AiFeignClient）
public class AiFeignConfig {

    // 连接超时与读取超时，可在 Nacos 中通过 oj.ai.client.* 覆盖
    @Bean
    public Request.Options aiRequestOptions(@Value("${oj.ai.client.connect-timeout-ms:3000}") long connectTimeoutMs,
                                            @Value("${oj.ai.client.read-timeout-ms:120000}") long readTimeoutMs) {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS, readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }
}
