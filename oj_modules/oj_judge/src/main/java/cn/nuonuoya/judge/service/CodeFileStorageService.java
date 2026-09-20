package cn.nuonuoya.judge.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 用户待评测代码本地持久化落盘管理服务
@Service
public class CodeFileStorageService {

    // 默认源码文件名称
    public static final String SOLUTION_FILE_NAME = "Solution.java";

    // 时间格式化器（年月日时分秒）
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 用户代码本地保存根目录
    @Value("${oj.judge.code-dir:./user-code}")
    private String codeDir;

    // 将用户完整源码持久化写入本地磁盘文件
    public File saveSolutionFile(Long userId, Long submitId, String completeCode) {
        if (StrUtil.isBlank(completeCode)) {
            throw new IllegalArgumentException("待保存的代码内容不能为空");
        }

        // 规范化根目录绝对路径
        File rootDir = new File(codeDir).getAbsoluteFile();
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        // 按照用户要求生成隔离子目录名称：{userId}_{yyyyMMddHHmmss}
        String idPrefix = userId != null ? String.valueOf(userId) : String.valueOf(submitId);
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String folderName = idPrefix + "_" + timestamp;

        File subDir = new File(rootDir, folderName);
        // 同一秒并发提交防重名保护
        if (subDir.exists()) {
            folderName = folderName + "_" + (System.currentTimeMillis() % 1000);
            subDir = new File(rootDir, folderName);
        }
        subDir.mkdirs();

        // 写入 Solution.java 文件
        File solutionFile = new File(subDir, SOLUTION_FILE_NAME);
        FileUtil.writeString(completeCode, solutionFile, StandardCharsets.UTF_8);

        return solutionFile;
    }

    // 清理指定评测临时目录
    public void deleteFolder(File folder) {
        if (folder != null && folder.exists()) {
            FileUtil.del(folder);
        }
    }
}
