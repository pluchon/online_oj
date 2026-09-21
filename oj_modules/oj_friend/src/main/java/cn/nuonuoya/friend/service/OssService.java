package cn.nuonuoya.friend.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.friend.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

// 阿里云OSS文件上传服务
@Slf4j
@Service
public class OssService {

    // 头像文件大小上限（2MB）
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;

    // 未配置存储目录时的默认头像目录
    private static final String DEFAULT_AVATAR_DIR = "online_oj/avatar/";

    // OSS配置属性
    @Autowired
    private OssProperties ossProperties;

    // 上传用户头像文件至阿里云OSS
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 校验文件大小
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 校验文件拓展名与MIME类型
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!isAllowedImageExtension(extension)) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 构造唯一存储文件名与目标路径
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        String dir = ossProperties.getAvatarDir();
        if (dir == null || dir.trim().isEmpty()) {
            dir = DEFAULT_AVATAR_DIR;
        }
        if (dir.startsWith("/")) {
            dir = dir.substring(1);
        }
        if (!dir.endsWith("/")) {
            dir = dir + "/";
        }
        String objectKey = dir + fileName;

        OSS ossClient = null;
        try (InputStream inputStream = file.getInputStream()) {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );

            // 设置内容元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (file.getContentType() != null) {
                metadata.setContentType(file.getContentType());
            }

            // 执行上传
            ossClient.putObject(ossProperties.getBucketName(), objectKey, inputStream, metadata);

            // 拼接完整公网访问URL
            String prefix = ossProperties.getUrlPrefix();
            if (prefix == null || prefix.trim().isEmpty()) {
                prefix = ossProperties.getEndpoint().replace("://", "://" + ossProperties.getBucketName() + ".");
            }
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
            String fileUrl = prefix + objectKey;
            log.info("用户头像上传OSS成功，存储路径：{}，访问地址：{}", objectKey, fileUrl);
            return fileUrl;
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("用户头像上传OSS失败，文件名：{}，原因：{}", originalFilename, e.getMessage(), e);
            throw new ServiceException(ResultCode.ERROR);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    // 校验图片后缀合法性
    private boolean isAllowedImageExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".webp".equals(extension)
                || ".gif".equals(extension);
    }
}
