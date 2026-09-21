package cn.nuonuoya.friend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// C端业务服务启动类
@SpringBootApplication
@MapperScan("cn.nuonuoya.**.mapper")
@EnableFeignClients(basePackages = "cn.nuonuoya.friend.client")
public class FriendApplication {
    public static void main(String[] args) {
        SpringApplication.run(FriendApplication.class, args);
    }
}
