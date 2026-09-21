package cn.nuonuoya.judge.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 评测代码落盘管理服务（每次评测使用独立目录，评测结束后清理）
@Service
public class CodeFileStorageService {

    // 源码文件名称
    public static final String SOLUTION_FILE_NAME = "Solution.java";

    // 用例标准输入文件名称
    public static final String INPUT_FILE_NAME = "input.txt";

    // 时间格式化器（年月日时分秒）
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 用户代码本地保存根目录
    @Value("${oj.judge.code-dir:./user-code}")
    private String codeDir;

    // 将完整源码写入独立评测目录：{userId或submitId}_{yyyyMMddHHmmss}_{随机串}/Solution.java
    public File saveSolutionFile(Long userId, Long submitId, String completeCode) {
        if (StrUtil.isBlank(completeCode)) {
            throw new IllegalArgumentException("待保存的代码内容不能为空");
        }
        String idPrefix = userId != null ? String.valueOf(userId) : String.valueOf(submitId);
        String folderName = idPrefix + "_" + LocalDateTime.now().format(TIME_FORMATTER) + "_" + IdUtil.fastSimpleUUID().substring(0, 8);
        File workDir = FileUtil.mkdir(new File(new File(codeDir).getAbsoluteFile(), folderName));

        File solutionFile = new File(workDir, SOLUTION_FILE_NAME);
        FileUtil.writeString(completeCode, solutionFile, StandardCharsets.UTF_8);
        return solutionFile;
    }

    // 将用例标准输入写入评测目录
    public File saveInputFile(File folder, String stdinContent) {
        File inputFile = new File(folder, INPUT_FILE_NAME);
        FileUtil.writeString(stdinContent, inputFile, StandardCharsets.UTF_8);
        return inputFile;
    }

    // 删除评测目录
    public void deleteFolder(File folder) {
        if (folder != null && folder.exists()) {
            FileUtil.del(folder);
        }
    }
}
