package cn.nuonuoya.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// 定时任务调度执行器微服务启动类（只负责按时触发，业务由各服务内部接口完成）
@SpringBootApplication
@EnableFeignClients(basePackages = "cn.nuonuoya.job.client")
public class JobApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobApplication.class, args);
    }
}
