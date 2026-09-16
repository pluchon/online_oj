package cn.nuonuoya.message.sms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// 短信服务自动装配配置类
@Configuration
@EnableConfigurationProperties(SmsProperties.class)
@ComponentScan(basePackages = "cn.nuonuoya.message.sms")
public class SmsAutoConfiguration {
}
