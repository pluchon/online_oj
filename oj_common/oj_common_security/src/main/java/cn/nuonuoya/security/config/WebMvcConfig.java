package cn.nuonuoya.security.config;

import cn.nuonuoya.security.GlobalExceptionHandler;
import cn.nuonuoya.security.interceptor.TokenInterceptor;
import cn.nuonuoya.security.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 安全模块自动装配：注册登录上下文拦截器、令牌服务与全局异常处理
@AutoConfiguration
@Import({TokenInterceptor.class, TokenService.class, GlobalExceptionHandler.class})
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;

    // 注册拦截器，登录与接口文档路径无需解析身份
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/**/login",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/webjars/**"
                );
    }
}
