package cn.nuonuoya.message.sms.config;

import cn.nuonuoya.message.sms.service.SmsService;
import cn.nuonuoya.message.sms.service.impl.AliyunSmsServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

// 短信服务自动装配
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsAutoConfiguration {

    // 阿里云短信服务
    @Bean
    public SmsService smsService() {
        return new AliyunSmsServiceImpl();
    }
}
