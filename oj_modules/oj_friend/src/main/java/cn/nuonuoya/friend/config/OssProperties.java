package cn.nuonuoya.friend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 阿里云OSS配置属性类
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    // OSS地域节点地址
    private String endpoint;

    // 阿里云AccessKey ID
    private String accessKeyId;

    // 阿里云AccessKey Secret
    private String accessKeySecret;

    // OSS Bucket存储空间名称
    private String bucketName;

    // OSS访问URL公网前缀
    private String urlPrefix;

    // 头像存储目录相对路径
    private String avatarDir = "online_oj/avatar/";
}
