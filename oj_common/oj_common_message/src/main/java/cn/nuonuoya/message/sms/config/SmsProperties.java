package cn.nuonuoya.message.sms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 阿里云短信配置属性类
@Getter
@Setter
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    // 阿里云AccessKey ID
    private String accessKeyId;

    // 阿里云AccessKey Secret
    private String accessKeySecret;

    // 短信签名名称
    private String signName = "速通互联验证码";

    // 短信模板CODE
    private String templateCode = "100001";

    // 阿里云号码认证服务接口地址
    private String endpoint = "dypnsapi.aliyuncs.com";

    // 验证码有效时间（单位：分钟）
    private Integer expireMin = 5;

    // 发送间隔冷却时间（单位：秒，默认60秒）
    private Long intervalSeconds = 60L;

    // 单手机号每天最大发送次数限制（默认10次）
    private Integer maxDailyCount = 10;

    // 是否真实发送短信（默认 true；false 为模拟发码模式，只写 Redis 不扣费）
    private Boolean isConfirm = true;
}
