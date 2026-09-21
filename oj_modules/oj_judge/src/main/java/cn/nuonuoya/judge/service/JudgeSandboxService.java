package cn.nuonuoya.judge.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.judge.dto.JudgeCaseDTO;
import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.enums.JudgePassEnum;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.api.judge.vo.JudgeCaseResultVO;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.judge.enums.QuestionDifficultyScoreEnum;
import cn.nuonuoya.judge.pool.DockerContainerPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// Docker 沙箱判题核心执行引擎服务（基于常驻容器池，单次 JVM 批量执行全部用例）
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
        List<JudgeCaseDTO> cases = requestDTO.getCases() != null ? requestDTO.getCases() : new ArrayList<>();

        JudgeResultVO resultVO = new JudgeResultVO();
        resultVO.setSubmitId(submitId);
        resultVO.setTotalCount(cases.size());
        resultVO.setPassCount(0);
        resultVO.setPass(JudgePassEnum.NOT_PASS.getCode());
        resultVO.setScore(0);
        resultVO.setTimeCost(0L);
        resultVO.setMemoryCost(0L);
        resultVO.setCaseResults(buildEmptyCaseResults(cases));

        if (CollUtil.isEmpty(cases)) {
            fillStatus(resultVO, JudgeStatusEnum.SE, "题目尚未配置测试用例");
            return resultVO;
        }

        // 步骤 1：用户代码与用例输入持久化落盘至 user-code/{userId}_{timestamp}/
        File solutionFile;
        try {
            solutionFile = codeFileStorageService.saveSolutionFile(userId, submitId, requestDTO.getCompleteCode());
            codeFileStorageService.saveInputFile(solutionFile.getParentFile(), buildStdin(cases));
        } catch (Exception e) {
            log.error("用户代码持久化保存失败, submitId: {}, error: {}", submitId, e.getMessage(), e);
            fillStatus(resultVO, JudgeStatusEnum.SE, "系统错误：代码落盘失败");
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
                fillStatus(resultVO, JudgeStatusEnum.SE, "系统繁忙：评测队列排队超时，请稍后重试");
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
                fillStatus(resultVO, JudgeStatusEnum.CE, "编译超时（超过 5 秒）");
                return resultVO;
            }

            if (compileResult.getExitCode() != 0) {
                fillStatus(resultVO, JudgeStatusEnum.CE, compileResult.getOutput());
                return resultVO;
            }

            // 步骤 4：第二阶段 - 在沙箱容器内执行 java 运行，全部用例经标准输入一次性喂入
            int runTimeoutMs = timeLimit + timeoutBufferMs;
            String runCmd = String.format("java -Xmx%dm -Xss256k Solution < %s",
                    spaceLimit, CodeFileStorageService.INPUT_FILE_NAME);

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
                fillStatus(resultVO, JudgeStatusEnum.TLE, "程序执行时间超过限制（" + timeLimit + " ms）");
                return resultVO;
            }

            // 步骤 5：逐行比对输出（每个用例对应一行）
            List<String> outputLines = splitLines(runResult.getOutput());
            boolean exitedNormally = runResult.getExitCode() == 0;
            // 异常退出时只有连续匹配的前缀行视为用例输出，其后均为错误信息
            int validLineCount = exitedNormally ? outputLines.size() : countLeadingMatches(cases, outputLines);
            int passCount = compareCases(cases, outputLines.subList(0, validLineCount), resultVO);
            resultVO.setPassCount(passCount);

            // 处理非零退出异常（RE 运行时异常 / MLE 内存溢出）
            if (!exitedNormally) {
                String output = runResult.getOutput();
                if (output.contains("OutOfMemoryError")) {
                    shouldEvict = true;
                    fillStatus(resultVO, JudgeStatusEnum.MLE, "程序占用内存超出限制（" + spaceLimit + " MB）");
                } else {
                    fillStatus(resultVO, JudgeStatusEnum.RE, extractErrorOutput(outputLines, validLineCount));
                }
                return resultVO;
            }

            if (passCount == cases.size()) {
                fillStatus(resultVO, JudgeStatusEnum.AC, null);
                resultVO.setPass(JudgePassEnum.PASS.getCode());
            } else {
                fillStatus(resultVO, JudgeStatusEnum.WA, null);
            }
            resultVO.setScore(calculateScore(difficulty, passCount, cases.size()));
            return resultVO;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fillStatus(resultVO, JudgeStatusEnum.SE, "系统中断异常");
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

    // 填充判题状态与回显信息
    private void fillStatus(JudgeResultVO resultVO, JudgeStatusEnum status, String exeMessage) {
        resultVO.setStatus(status.getCode());
        resultVO.setStatusDesc(status.getName());
        resultVO.setExeMessage(exeMessage);
    }

    // 组装标准输入：首行用例数，其后依次为各用例输入
    private String buildStdin(List<JudgeCaseDTO> cases) {
        StringBuilder sb = new StringBuilder();
        sb.append(cases.size()).append('\n');
        for (JudgeCaseDTO judgeCase : cases) {
            String input = judgeCase.getInput() == null ? "" : judgeCase.getInput().replace("\r", "");
            sb.append(input).append('\n');
        }
        return sb.toString();
    }

    // 初始化逐用例结果（默认未通过、无输出）
    private List<JudgeCaseResultVO> buildEmptyCaseResults(List<JudgeCaseDTO> cases) {
        List<JudgeCaseResultVO> caseResults = new ArrayList<>(cases.size());
        for (JudgeCaseDTO judgeCase : cases) {
            JudgeCaseResultVO caseResult = new JudgeCaseResultVO();
            caseResult.setCaseId(judgeCase.getCaseId());
            caseResult.setPass(false);
            caseResults.add(caseResult);
        }
        return caseResults;
    }

    // 按行拆分程序输出并去除行尾空白
    private List<String> splitLines(String output) {
        List<String> lines = new ArrayList<>();
        if (StrUtil.isEmpty(output)) {
            return lines;
        }
        for (String line : output.replace("\r", "").split("\n", -1)) {
            lines.add(StrUtil.trimEnd(line));
        }
        // 去掉末尾换行产生的空行
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    // 统计从首行起连续与预期一致的输出行数
    private int countLeadingMatches(List<JudgeCaseDTO> cases, List<String> outputLines) {
        int count = 0;
        while (count < cases.size() && count < outputLines.size()
                && outputLines.get(count).trim().equals(StrUtil.trim(cases.get(count).getExpectedOutput()))) {
            count++;
        }
        return count;
    }

    // 逐用例比对输出，回填逐用例结果与首个未通过用例，返回通过数
    private int compareCases(List<JudgeCaseDTO> cases, List<String> outputLines, JudgeResultVO resultVO) {
        int passCount = 0;
        List<JudgeCaseResultVO> caseResults = resultVO.getCaseResults();
        for (int i = 0; i < cases.size(); i++) {
            String expected = StrUtil.trim(cases.get(i).getExpectedOutput());
            String actual = i < outputLines.size() ? outputLines.get(i) : null;
            boolean pass = actual != null && actual.trim().equals(expected);
            JudgeCaseResultVO caseResult = caseResults.get(i);
            caseResult.setActualOutput(actual);
            caseResult.setPass(pass);
            if (pass) {
                passCount++;
            } else if (resultVO.getFailCaseId() == null) {
                resultVO.setFailCaseId(cases.get(i).getCaseId());
                resultVO.setFailOutput(actual);
            }
        }
        return passCount;
    }

    // 提取运行异常时的错误输出（跳过已通过用例的正常输出行）
    private String extractErrorOutput(List<String> outputLines, int validLineCount) {
        if (outputLines.size() <= validLineCount) {
            return "程序异常退出";
        }
        return String.join("\n", outputLines.subList(validLineCount, outputLines.size()));
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
        int baseScore = QuestionDifficultyScoreEnum.getFullScore(difficulty);
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
