package cn.nuonuoya.friend;

import cn.nuonuoya.security.config.WebMvcConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

// C端业务服务启动类
@SpringBootApplication
@MapperScan("cn.nuonuoya.**.mapper")
@EnableFeignClients(basePackages = "cn.nuonuoya.friend.client")
@Import(WebMvcConfig.class)
public class FriendApplication {
    public static void main(String[] args) {
        SpringApplication.run(FriendApplication.class, args);
    }
}
