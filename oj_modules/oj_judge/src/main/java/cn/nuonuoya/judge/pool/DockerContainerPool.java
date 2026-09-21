package cn.nuonuoya.judge.pool;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Docker 沙箱常驻容器池管理器
@Slf4j
@Component
public class DockerContainerPool {

    // 沙箱容器名称前缀
    private static final String CONTAINER_PREFIX = "oj_worker_";

    // 容器内评测工作区根目录（容器私有，不挂载宿主机目录）
    public static final String SANDBOX_ROOT = "/sandbox";

    // 创建容器的等待时间（秒）
    private static final long CREATE_TIMEOUT_SECONDS = 10;

    // 销毁容器的等待时间（秒）
    private static final long DESTROY_TIMEOUT_SECONDS = 5;

    // 空闲容器队列
    private final BlockingQueue<String> pool = new LinkedBlockingQueue<>();

    // 递增计数器用于命名
    private final AtomicInteger counter = new AtomicInteger(1);

    // 用户代码本地暂存根目录（评测时拷贝进容器）
    @Value("${oj.judge.code-dir:./user-code}")
    private String codeDir;

    // 沙箱执行镜像
    @Value("${oj.judge.docker.image:maven:3.9-eclipse-temurin-17-alpine}")
    private String dockerImage;

    // 常驻容器池大小
    @Value("${oj.judge.docker.pool-size:3}")
    private int poolSize;

    // 初始化容器池，预热常驻沙箱容器
    @PostConstruct
    public void initPool() {
        log.info("开始初始化 Docker 沙箱容器池, 目标池大小: {}", poolSize);
        File rootDir = new File(codeDir).getAbsoluteFile();
        if (!rootDir.exists() && !rootDir.mkdirs()) {
            log.error("创建沙箱挂载目录失败: {}", rootDir);
        }

        // 清理上次运行残留的容器
        cleanHistoricalContainers();

        for (int i = 1; i <= poolSize; i++) {
            String containerName = createNewContainer();
            if (containerName != null) {
                pool.offer(containerName);
            }
        }
        log.info("Docker 沙箱容器池初始化完成, 当前可用容器数: {}", pool.size());
    }

    // 借出一个空闲容器，超时返回 null
    public String borrowContainer(long timeoutMs) throws InterruptedException {
        return pool.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    // 归还容器
    public void returnContainer(String containerName) {
        if (containerName != null) {
            pool.offer(containerName);
        }
    }

    // 销毁异常或超时的容器，并补充新容器入池
    public void evictAndReplaceContainer(String poisonedContainerName) {
        log.warn("检测到异常/超时容器: {}, 执行销毁并补充新容器", poisonedContainerName);
        destroyContainer(poisonedContainerName);
        String newContainerName = createNewContainer();
        if (newContainerName != null) {
            pool.offer(newContainerName);
            log.info("新沙箱容器补充入池成功: {}", newContainerName);
        }
    }

    // 将本地评测目录拷贝到容器私有工作区 /sandbox/{目录名}，成功返回 true
    public boolean copyIntoContainer(String containerName, File workDir) {
        return runDockerCommand(CREATE_TIMEOUT_SECONDS,
                "docker", "cp", workDir.getAbsolutePath(), containerName + ":" + SANDBOX_ROOT + "/");
    }

    // 清理容器工作区：结束所有残留进程并删除评测目录，成功返回 true（失败时调用方应淘汰该容器）
    public boolean cleanWorkspace(String containerName, String folderName) {
        return runDockerCommand(DESTROY_TIMEOUT_SECONDS,
                "docker", "exec", containerName, "sh", "-c",
                "rm -rf " + SANDBOX_ROOT + "/" + folderName + "; kill -9 -1 2>/dev/null; true");
    }

    // 执行一条 docker 命令，按时完成且退出码为 0 返回 true
    private boolean runDockerCommand(long timeoutSeconds, String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("docker 命令执行超时: {}", String.join(" ", command));
                return false;
            }
            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                log.warn("docker 命令执行失败: exitCode = {}, output = {}", process.exitValue(), output);
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("docker 命令执行异常: {}", e.getMessage());
            return false;
        }
    }

    // 创建并启动一个常驻待命的无网络沙箱容器（不挂载宿主机目录），失败返回 null
    public synchronized String createNewContainer() {
        String containerName = CONTAINER_PREFIX + counter.getAndIncrement() + "_" + (System.currentTimeMillis() % 100000);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run",
                    "-d",
                    "--name", containerName,
                    "--network", "none",
                    "--pids-limit", "64",
                    "--memory", "256m",
                    "--memory-swap", "256m",
                    "--cpus", "1.0",
                    dockerImage,
                    "sh", "-c", "mkdir -p " + SANDBOX_ROOT + " && exec tail -f /dev/null"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            if (!process.waitFor(CREATE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.error("创建沙箱容器超时: {}", containerName);
                return null;
            }
            if (process.exitValue() == 0) {
                log.info("成功创建常驻沙箱容器: {}", containerName);
                return containerName;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            log.error("创建沙箱容器失败: exitCode = {}, output = {}", process.exitValue(), output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("创建沙箱容器被中断: {}", containerName);
        } catch (Exception e) {
            log.error("创建沙箱容器发生异常: {}", e.getMessage(), e);
        }
        return null;
    }

    // 强制销毁指定容器
    public void destroyContainer(String containerName) {
        try {
            new ProcessBuilder("docker", "rm", "-f", containerName)
                    .start()
                    .waitFor(DESTROY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("销毁沙箱容器被中断: {}", containerName);
        } catch (Exception e) {
            log.warn("销毁沙箱容器失败: {}, error = {}", containerName, e.getMessage());
        }
    }

    // 清理上次运行残留的沙箱容器
    private void cleanHistoricalContainers() {
        try {
            Process p = new ProcessBuilder("docker", "ps", "-a", "--filter", "name=" + CONTAINER_PREFIX, "-q").start();
            String ids = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor(DESTROY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!ids.isEmpty()) {
                for (String id : ids.split("\\s+")) {
                    destroyContainer(id);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("清理历史沙箱容器被中断");
        } catch (Exception e) {
            log.warn("清理历史沙箱容器失败: {}", e.getMessage());
        }
    }

    // 服务关闭时销毁池中空闲容器（借出中的容器由下次启动时的残留清理处理）
    @PreDestroy
    public void destroyPool() {
        log.info("判题服务正在关闭，开始销毁常驻沙箱容器...");
        String name;
        while ((name = pool.poll()) != null) {
            destroyContainer(name);
        }
        log.info("常驻沙箱容器已清理完成");
    }
}
