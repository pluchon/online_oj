package cn.nuonuoya.judge.service;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.judge.pool.DockerContainerPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// Docker 沙箱判题核心执行引擎服务（基于常驻容器池）
@Slf4j
@Service
public class JudgeSandboxService {

    // 注入代码持久化落盘服务
    @Autowired
    private CodeFileStorageService codeFileStorageService;

    // 注入常驻沙箱容器池管理器
    @Autowired
    private DockerContainerPool dockerContainerPool;

    // 判题执行超时缓冲毫秒数
    @Value("${oj.judge.docker.timeout-buffer-ms:500}")
    private int timeoutBufferMs;

    // 最大日志输出截取字符长度
    @Value("${oj.judge.docker.max-output-length:8192}")
    private int maxOutputLength;

    // 核心评测执行入口方法
    public JudgeResultVO executeJudge(JudgeRequestDTO requestDTO) {
        Long submitId = requestDTO.getSubmitId();
        Long userId = requestDTO.getUserId();
        Integer timeLimit = requestDTO.getTimeLimit() != null ? requestDTO.getTimeLimit() : 1000;
        Integer spaceLimit = requestDTO.getSpaceLimit() != null ? requestDTO.getSpaceLimit() : 128;
        Integer difficulty = requestDTO.getDifficulty();

        JudgeResultVO resultVO = new JudgeResultVO();
        resultVO.setSubmitId(submitId);
        resultVO.setTotalCount(1);
        resultVO.setPassCount(0);
        resultVO.setPass(0);
        resultVO.setScore(0);
        resultVO.setTimeCost(0L);
        resultVO.setMemoryCost(0L);

        // 步骤 1：用户代码持久化落盘至 user-code/{userId}_{timestamp}/Solution.java
        File solutionFile;
        try {
            solutionFile = codeFileStorageService.saveSolutionFile(userId, submitId, requestDTO.getCompleteCode());
        } catch (Exception e) {
            log.error("用户代码持久化保存失败, submitId: {}, error: {}", submitId, e.getMessage(), e);
            resultVO.setStatus(JudgeStatusEnum.SE.getCode());
            resultVO.setStatusDesc(JudgeStatusEnum.SE.getName());
            resultVO.setExeMessage("系统错误：代码落盘失败");
            return resultVO;
        }

        // 获取隔离子目录名称，用于在共享挂载卷中定位
        String subFolderName = solutionFile.getParentFile().getName();

        // 步骤 2：从容器池中借用一个已预热就绪的常驻沙箱容器（免除频繁起停容器）
        String containerName = null;
        boolean shouldEvict = false;
        try {
            containerName = dockerContainerPool.borrowContainer(5000);
            if (containerName == null) {
                log.warn("沙箱容器池暂无可用容器, submitId: {}", submitId);
                resultVO.setStatus(JudgeStatusEnum.SE.getCode());
                resultVO.setStatusDesc(JudgeStatusEnum.SE.getName());
                resultVO.setExeMessage("系统繁忙：沙箱评测队列排队超时，请稍后重试");
                return resultVO;
            }

            // 步骤 3：第一阶段 - 在沙箱容器内执行 javac 编译检查
            ProcessResult compileResult = runDockerExecCommand(
                    containerName,
                    subFolderName,
                    5000,
                    "javac Solution.java"
            );

            if (compileResult.isTimeout()) {
                shouldEvict = true;
                resultVO.setStatus(JudgeStatusEnum.CE.getCode());
                resultVO.setStatusDesc(JudgeStatusEnum.CE.getName());
                resultVO.setExeMessage("编译错误 (Compile Error)：javac 编译超时（超过5秒）");
                return resultVO;
            }

            if (compileResult.getExitCode() != 0) {
                resultVO.setStatus(JudgeStatusEnum.CE.getCode());
                resultVO.setStatusDesc(JudgeStatusEnum.CE.getName());
                resultVO.setExeMessage("编译错误 (Compile Error)：\n" + compileResult.getOutput());
                return resultVO;
            }

            // 步骤 4：第二阶段 - 在沙箱容器内执行 java 运行
            int runTimeoutMs = timeLimit + timeoutBufferMs;
            String runCmd = String.format("java -Xmx%dm -Xss256k Solution", spaceLimit);

            ProcessResult runResult = runDockerExecCommand(
                    containerName,
                    subFolderName,
                    runTimeoutMs,
                    runCmd
            );

            resultVO.setTimeCost(runResult.getDurationMs());
            resultVO.setMemoryCost((long) spaceLimit);

            // 处理死循环超时（TLE）：必须淘汰并强杀该容器，防止后台死循环进程残留
            if (runResult.isTimeout()) {
                shouldEvict = true;
                resultVO.setStatus(JudgeStatusEnum.TLE.getCode());
                resultVO.setStatusDesc(JudgeStatusEnum.TLE.getName());
                resultVO.setExeMessage("运行超时 (Time Limit Exceeded)：程序执行时间超过限制 (" + timeLimit + " ms)");
                return resultVO;
            }

            // 处理非零退出异常（RE 运行时异常 / MLE 内存溢出）
            if (runResult.getExitCode() != 0) {
                String output = runResult.getOutput();
                if (output.contains("OutOfMemoryError")) {
                    shouldEvict = true;
                    resultVO.setStatus(JudgeStatusEnum.MLE.getCode());
                    resultVO.setStatusDesc(JudgeStatusEnum.MLE.getName());
                    resultVO.setExeMessage("内存超限 (Memory Limit Exceeded)：程序占用内存超出限制 (" + spaceLimit + " MB)");
                } else {
                    resultVO.setStatus(JudgeStatusEnum.RE.getCode());
                    resultVO.setStatusDesc(JudgeStatusEnum.RE.getName());
                    resultVO.setExeMessage("运行异常 (Runtime Error)：\n" + output);
                }
                return resultVO;
            }

            // 步骤 5：对比输出结果并计算得分
            String stdout = runResult.getOutput().trim();
            if (stdout.contains("OK")) {
                resultVO.setStatus(JudgeStatusEnum.AC.getCode());
                resultVO.setStatusDesc(JudgeStatusEnum.AC.getName());
                resultVO.setPass(1);
                resultVO.setPassCount(1);
                resultVO.setScore(calculateScore(difficulty, 1, 1));
                resultVO.setExeMessage("运行通过 (Accepted) - 执行耗时: " + runResult.getDurationMs() + "ms");
            } else {
                resultVO.setStatus(JudgeStatusEnum.WA.getCode());
                resultVO.setStatusDesc(JudgeStatusEnum.WA.getName());
                resultVO.setPass(0);
                resultVO.setPassCount(0);
                resultVO.setScore(0);
                resultVO.setExeMessage("答案错误 (Wrong Answer)：\n" + stdout);
            }

            return resultVO;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resultVO.setStatus(JudgeStatusEnum.SE.getCode());
            resultVO.setStatusDesc(JudgeStatusEnum.SE.getName());
            resultVO.setExeMessage("系统中断异常");
            return resultVO;
        } finally {
            // 归还或淘汰自愈
            if (containerName != null) {
                if (shouldEvict) {
                    dockerContainerPool.evictAndReplaceContainer(containerName);
                } else {
                    dockerContainerPool.returnContainer(containerName);
                }
            }
        }
    }

    // 在常驻就绪容器内极速执行命令（避免重新启动容器）
    private ProcessResult runDockerExecCommand(
            String containerName,
            String subFolderName,
            int timeoutMs,
            String innerCommand
    ) {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec",
                "-w", "/sandbox/" + subFolderName,
                containerName,
                "sh", "-c", innerCommand
        );

        // 合并标准输出与标准错误输出，彻底消除 Pipe 阻塞死锁
        pb.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        Process process = null;
        try {
            process = pb.start();
            final Process p = process;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 独立异步线程按上限读取输出流，避免缓冲区打满
            CompletableFuture<Void> readFuture = CompletableFuture.runAsync(() -> {
                try (InputStream is = p.getInputStream()) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        if (baos.size() < maxOutputLength) {
                            int writeLen = Math.min(len, maxOutputLength - baos.size());
                            baos.write(buf, 0, writeLen);
                        }
                    }
                } catch (Exception ignored) {
                }
            });

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long costTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                readFuture.cancel(true);
                return new ProcessResult(true, -1, costTime, "Execution Timeout");
            }

            try {
                readFuture.get(500, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
            }

            int exitCode = process.exitValue();
            String output = baos.toString(StandardCharsets.UTF_8);
            return new ProcessResult(false, exitCode, costTime, output);

        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            long costTime = System.currentTimeMillis() - startTime;
            return new ProcessResult(false, -1, costTime, "进程执行异常: " + e.getMessage());
        }
    }

    // 根据题目难度与通过用例数计算实际得分
    private int calculateScore(Integer difficulty, int passCount, int totalCount) {
        int baseScore = 100;
        if (difficulty != null) {
            if (difficulty == 2) {
                baseScore = 200;
            } else if (difficulty == 3) {
                baseScore = 300;
            }
        }
        if (totalCount <= 0) {
            return 0;
        }
        return (int) Math.round((double) baseScore * passCount / totalCount);
    }

    // 子进程运行结果结构体
    private static class ProcessResult {
        private final boolean timeout;
        private final int exitCode;
        private final long durationMs;
        private final String output;

        public ProcessResult(boolean timeout, int exitCode, long durationMs, String output) {
            this.timeout = timeout;
            this.exitCode = exitCode;
            this.durationMs = durationMs;
            this.output = output != null ? output : StrUtil.EMPTY;
        }

        public boolean isTimeout() {
            return timeout;
        }

        public int getExitCode() {
            return exitCode;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public String getOutput() {
            return output;
        }
    }
}
