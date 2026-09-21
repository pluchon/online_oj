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

    // 请求未携带时间限制时的默认值（毫秒）
    private static final int DEFAULT_TIME_LIMIT_MS = 1000;

    // 请求未携带空间限制时的默认值（MB）
    private static final int DEFAULT_SPACE_LIMIT_MB = 128;

    // 编译超时时间（毫秒）
    private static final int COMPILE_TIMEOUT_MS = 5000;

    // 等待空闲沙箱容器的最长时间（毫秒）
    private static final long BORROW_TIMEOUT_MS = 5000;

    // 容器被系统 OOM 杀死时的退出码
    private static final int OOM_KILLED_EXIT_CODE = 137;

    // 输出流读取完成的等待时间（毫秒）
    private static final long OUTPUT_DRAIN_TIMEOUT_MS = 500;

    @Autowired
    private CodeFileStorageService codeFileStorageService;

    @Autowired
    private DockerContainerPool dockerContainerPool;

    // 运行超时缓冲毫秒数（抵消 docker exec 与 JVM 启动开销）
    @Value("${oj.judge.docker.timeout-buffer-ms:500}")
    private int timeoutBufferMs;

    // 程序输出上限（字符），超出判为输出超限
    @Value("${oj.judge.docker.max-output-length:8192}")
    private int maxOutputLength;

    // 核心评测执行入口方法
    public JudgeResultVO executeJudge(JudgeRequestDTO requestDTO) {
        Long submitId = requestDTO.getSubmitId();
        int timeLimit = requestDTO.getTimeLimit() != null ? requestDTO.getTimeLimit() : DEFAULT_TIME_LIMIT_MS;
        int spaceLimit = requestDTO.getSpaceLimit() != null ? requestDTO.getSpaceLimit() : DEFAULT_SPACE_LIMIT_MB;
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

        // 步骤 1：用户代码与用例输入落盘至独立评测目录
        File workDir;
        try {
            File solutionFile = codeFileStorageService.saveSolutionFile(requestDTO.getUserId(), submitId, requestDTO.getCompleteCode());
            workDir = solutionFile.getParentFile();
            codeFileStorageService.saveInputFile(workDir, buildStdin(cases));
        } catch (Exception e) {
            log.error("用户代码持久化保存失败, submitId: {}", submitId, e);
            fillStatus(resultVO, JudgeStatusEnum.SE, "系统错误：代码落盘失败");
            return resultVO;
        }

        String containerName = null;
        boolean shouldEvict = false;
        try {
            // 步骤 2：借用已预热的常驻沙箱容器
            containerName = dockerContainerPool.borrowContainer(BORROW_TIMEOUT_MS);
            if (containerName == null) {
                log.warn("沙箱容器池暂无可用容器, submitId: {}", submitId);
                fillStatus(resultVO, JudgeStatusEnum.SE, "系统繁忙：评测队列排队超时，请稍后重试");
                return resultVO;
            }

            // 将评测文件拷入该容器的私有工作区（容器间不共享任何目录）
            if (!dockerContainerPool.copyIntoContainer(containerName, workDir)) {
                shouldEvict = true;
                fillStatus(resultVO, JudgeStatusEnum.SE, "系统错误：沙箱执行失败");
                return resultVO;
            }

            // 步骤 3：编译
            ProcessResult compileResult = runDockerExecCommand(containerName, workDir.getName(), COMPILE_TIMEOUT_MS, "javac Solution.java");
            if (compileResult.isSystemError()) {
                shouldEvict = true;
                fillStatus(resultVO, JudgeStatusEnum.SE, "系统错误：沙箱执行失败");
                return resultVO;
            }
            if (compileResult.isTimeout()) {
                shouldEvict = true;
                fillStatus(resultVO, JudgeStatusEnum.CE, "编译超时（超过 " + COMPILE_TIMEOUT_MS / 1000 + " 秒）");
                return resultVO;
            }
            if (compileResult.getExitCode() != 0) {
                fillStatus(resultVO, JudgeStatusEnum.CE, compileResult.getOutput());
                return resultVO;
            }

            // 步骤 4：运行，全部用例经标准输入一次性喂入
            String runCmd = String.format("java -Xmx%dm -Xss256k Solution < %s", spaceLimit, CodeFileStorageService.INPUT_FILE_NAME);
            ProcessResult runResult = runDockerExecCommand(containerName, workDir.getName(), timeLimit + timeoutBufferMs, runCmd);
            resultVO.setTimeCost(runResult.getDurationMs());
            resultVO.setMemoryCost((long) spaceLimit);

            if (runResult.isSystemError()) {
                shouldEvict = true;
                fillStatus(resultVO, JudgeStatusEnum.SE, "系统错误：沙箱执行失败");
                return resultVO;
            }
            // 超时后容器内进程可能仍在运行，必须淘汰该容器
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

            if (!exitedNormally) {
                if (runResult.getExitCode() == OOM_KILLED_EXIT_CODE || runResult.getOutput().contains("OutOfMemoryError")) {
                    shouldEvict = true;
                    fillStatus(resultVO, JudgeStatusEnum.MLE, "程序占用内存超出限制（" + spaceLimit + " MB）");
                } else {
                    fillStatus(resultVO, JudgeStatusEnum.RE, extractErrorOutput(outputLines, validLineCount));
                }
                return resultVO;
            }
            if (runResult.isTruncated()) {
                fillStatus(resultVO, JudgeStatusEnum.OLE, "程序输出超过限制（" + maxOutputLength + " 字符）");
                return resultVO;
            }

            if (passCount == cases.size()) {
                fillStatus(resultVO, JudgeStatusEnum.AC, null);
                resultVO.setPass(JudgePassEnum.PASS.getCode());
            } else {
                fillStatus(resultVO, JudgeStatusEnum.WA, null);
            }
            resultVO.setScore(calculateScore(requestDTO.getDifficulty(), passCount, cases.size()));
            return resultVO;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fillStatus(resultVO, JudgeStatusEnum.SE, "系统中断异常");
            return resultVO;
        } finally {
            if (containerName != null) {
                // 归还前清理残留进程与评测目录，避免下一次评测读取到本次代码；清理失败则直接淘汰容器
                if (!shouldEvict && !dockerContainerPool.cleanWorkspace(containerName, workDir.getName())) {
                    shouldEvict = true;
                }
                if (shouldEvict) {
                    dockerContainerPool.evictAndReplaceContainer(containerName);
                } else {
                    dockerContainerPool.returnContainer(containerName);
                }
            }
            // 评测结束即清理代码与输入，源码已在提交记录中留存
            codeFileStorageService.deleteFolder(workDir);
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

    // 在常驻容器内执行命令，按上限收集合并后的标准输出与错误输出
    private ProcessResult runDockerExecCommand(String containerName, String subFolderName, long timeoutMs, String innerCommand) {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec",
                "-w", DockerContainerPool.SANDBOX_ROOT + "/" + subFolderName,
                containerName,
                "sh", "-c", innerCommand
        );
        // 合并标准输出与错误输出，避免两路管道互相阻塞
        pb.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        Process process = null;
        try {
            process = pb.start();
            final Process p = process;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean[] truncated = {false};

            // 独立线程持续读取输出，超出上限的部分丢弃，防止管道缓冲区写满导致子进程阻塞
            CompletableFuture<Void> readFuture = CompletableFuture.runAsync(() -> {
                try (InputStream is = p.getInputStream()) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        int remain = maxOutputLength - baos.size();
                        if (len > remain) {
                            truncated[0] = true;
                        }
                        if (remain > 0) {
                            baos.write(buf, 0, Math.min(len, remain));
                        }
                    }
                } catch (Exception e) {
                    // 进程被强制结束时流会被关闭，属于预期情况
                    log.debug("读取沙箱输出流结束: {}", e.getMessage());
                }
            });

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long costTime = System.currentTimeMillis() - startTime;
            if (!finished) {
                process.destroyForcibly();
                readFuture.cancel(true);
                return ProcessResult.timeout(costTime);
            }

            try {
                readFuture.get(OUTPUT_DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // 进程已结束但输出未在时限内读完，按已读取部分处理
                log.warn("沙箱输出读取未在 {} ms 内完成, container: {}", OUTPUT_DRAIN_TIMEOUT_MS, containerName);
            }
            return ProcessResult.finished(process.exitValue(), costTime, baos.toString(StandardCharsets.UTF_8), truncated[0]);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return ProcessResult.systemError(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            log.error("沙箱命令执行失败, container: {}", containerName, e);
            return ProcessResult.systemError(System.currentTimeMillis() - startTime);
        }
    }

    // 根据题目难度与通过用例数计算实际得分
    private int calculateScore(Integer difficulty, int passCount, int totalCount) {
        if (totalCount <= 0) {
            return 0;
        }
        int fullScore = QuestionDifficultyScoreEnum.getFullScore(difficulty);
        return (int) Math.round((double) fullScore * passCount / totalCount);
    }

    // 沙箱子进程执行结果
    private static class ProcessResult {

        // 是否执行超时
        private final boolean timeout;

        // 是否因沙箱自身故障未能执行（与用户代码无关）
        private final boolean systemError;

        // 进程退出码
        private final int exitCode;

        // 执行耗时（毫秒）
        private final long durationMs;

        // 合并后的输出
        private final String output;

        // 输出是否因超过上限被截断
        private final boolean truncated;

        private ProcessResult(boolean timeout, boolean systemError, int exitCode, long durationMs, String output, boolean truncated) {
            this.timeout = timeout;
            this.systemError = systemError;
            this.exitCode = exitCode;
            this.durationMs = durationMs;
            this.output = output != null ? output : StrUtil.EMPTY;
            this.truncated = truncated;
        }

        // 正常结束
        static ProcessResult finished(int exitCode, long durationMs, String output, boolean truncated) {
            return new ProcessResult(false, false, exitCode, durationMs, output, truncated);
        }

        // 执行超时
        static ProcessResult timeout(long durationMs) {
            return new ProcessResult(true, false, -1, durationMs, null, false);
        }

        // 沙箱故障
        static ProcessResult systemError(long durationMs) {
            return new ProcessResult(false, true, -1, durationMs, null, false);
        }

        boolean isTimeout() {
            return timeout;
        }

        boolean isSystemError() {
            return systemError;
        }

        int getExitCode() {
            return exitCode;
        }

        long getDurationMs() {
            return durationMs;
        }

        String getOutput() {
            return output;
        }

        boolean isTruncated() {
            return truncated;
        }
    }
}
