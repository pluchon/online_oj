package cn.nuonuoya.job.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// XXL-JOB 任务调度执行器配置类
@Configuration
@Slf4j
public class XxlJobConfig {

    // 调度中心部署地址
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    // 执行器通讯TOKEN
    @Value("${xxl.job.accessToken}")
    private String accessToken;

    // 执行器AppName
    @Value("${xxl.job.executor.appname}")
    private String appname;

    // 执行器端口
    @Value("${xxl.job.executor.port:9999}")
    private int port;

    // 执行器日志存储路径
    @Value("${xxl.job.executor.logpath:./logs/oj_job}")
    private String logPath;

    // 初始化 XXL-JOB 执行器组件
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setLogPath(logPath);
        return xxlJobSpringExecutor;
    }
}
