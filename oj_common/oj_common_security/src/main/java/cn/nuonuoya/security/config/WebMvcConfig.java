package cn.nuonuoya.security.config;

import cn.nuonuoya.security.GlobalExceptionHandler;
import cn.nuonuoya.security.interceptor.TokenInterceptor;
import cn.nuonuoya.security.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Web MVC 配置，注册全局拦截器
@AutoConfiguration
@Import({TokenInterceptor.class, TokenService.class, GlobalExceptionHandler.class})
public class WebMvcConfig implements WebMvcConfigurer {

    // Token拦截器
    @Autowired
    private TokenInterceptor tokenInterceptor;

    // 注册拦截器并配置拦截与排除路径
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
