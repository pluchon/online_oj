package cn.nuonuoya.judge.pool;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Docker 沙箱常驻容器池管理器
@Slf4j
@Component
public class DockerContainerPool {

    // 容器池阻塞队列
    private final BlockingQueue<String> pool = new LinkedBlockingQueue<>();

    // 递增计数器用于命名
    private final AtomicInteger counter = new AtomicInteger(1);

    // 用户代码本地保存根目录
    @Value("${oj.judge.code-dir:./user-code}")
    private String codeDir;

    // 默认执行镜像
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
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        // 清理历史残留的同名容器
        cleanHistoricalContainers();

        // 预热指定数量的常驻容器
        for (int i = 1; i <= poolSize; i++) {
            String containerName = createNewContainer();
            if (containerName != null) {
                pool.offer(containerName);
            }
        }
        log.info("Docker 沙箱容器池初始化完成, 当前可用容器数: {}", pool.size());
    }

    // 从容器池中借出一个可用沙箱容器
    public String borrowContainer(long timeoutMs) throws InterruptedException {
        return pool.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    // 正常执行完毕后归还容器至池中
    public void returnContainer(String containerName) {
        if (containerName != null) {
            pool.offer(containerName);
        }
    }

    // 淘汰中毒/超时容器并自动补充全新容器入池
    public void evictAndReplaceContainer(String poisonedContainerName) {
        log.warn("检测到异常/超时容器: {}, 执行物理销毁并补充新容器", poisonedContainerName);
        destroyContainer(poisonedContainerName);
        String newContainerName = createNewContainer();
        if (newContainerName != null) {
            pool.offer(newContainerName);
            log.info("全新沙箱容器补充入池成功: {}", newContainerName);
        }
    }

    // 创建并启动一个常驻待命的无网络沙箱容器
    public synchronized String createNewContainer() {
        String containerName = "oj_worker_" + counter.getAndIncrement() + "_" + (System.currentTimeMillis() % 100000);
        File rootDir = new File(codeDir).getAbsoluteFile();
        String hostMountDir = rootDir.getAbsolutePath().replace("\\", "/");

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
                    "-v", hostMountDir + ":/sandbox",
                    dockerImage,
                    "tail", "-f", "/dev/null"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean ok = process.waitFor(10, TimeUnit.SECONDS);
            if (ok && process.exitValue() == 0) {
                log.info("成功创建常驻沙箱容器: {}", containerName);
                return containerName;
            } else {
                log.error("创建沙箱容器失败: exitCode = {}", process.exitValue());
            }
        } catch (Exception e) {
            log.error("创建沙箱容器发生异常: {}", e.getMessage(), e);
        }
        return null;
    }

    // 销毁指定容器
    public void destroyContainer(String containerName) {
        try {
            new ProcessBuilder("docker", "rm", "-f", containerName)
                    .start()
                    .waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    // 清理历史残留容器
    private void cleanHistoricalContainers() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", "-a", "--filter", "name=oj_worker_", "-q");
            Process p = pb.start();
            String ids = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(5, TimeUnit.SECONDS);
            if (!ids.isEmpty()) {
                for (String id : ids.split("\\s+")) {
                    new ProcessBuilder("docker", "rm", "-f", id).start().waitFor(3, TimeUnit.SECONDS);
                }
            }
        } catch (Exception ignored) {
        }
    }

    // 容器池优雅销毁钩子
    @PreDestroy
    public void destroyPool() {
        log.info("判题服务正在关闭，开始销毁所有常驻沙箱容器...");
        while (!pool.isEmpty()) {
            String name = pool.poll();
            destroyContainer(name);
        }
        log.info("所有常驻沙箱容器已成功清理销毁");
    }
}
