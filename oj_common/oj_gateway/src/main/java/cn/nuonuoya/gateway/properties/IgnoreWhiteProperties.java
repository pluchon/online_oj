package cn.nuonuoya.gateway.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

// 网关免登录白名单配置（security.ignore.whites，由 Nacos 下发并支持动态刷新）
@Setter
@Getter
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreWhiteProperties {

    // 免登录路径规则（Ant 风格）
    private List<String> whites = new ArrayList<>();

}