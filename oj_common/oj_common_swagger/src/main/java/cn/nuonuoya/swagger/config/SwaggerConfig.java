package cn.nuonuoya.swagger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

// 接口文档自动装配（各服务引入 oj_common_swagger 即生效）
@AutoConfiguration
public class SwaggerConfig {

    // 接口文档基础信息
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("在线oj系统")
                        .description("在线oj系统接口文档")
                        .version("v1"));
    }
}
