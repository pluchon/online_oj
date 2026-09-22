package cn.nuonuoya.friend.search;

import cn.nuonuoya.friend.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

// 启动后在后台同步一次题目索引，补齐缺失的题目向量（文本未变的题目复用已有向量，不重复计算）
@Slf4j
@Component
public class QuestionIndexInitializer {

    @Autowired
    private QuestionService questionService;

    // 应用就绪后异步同步，失败不影响启动
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        CompletableFuture.runAsync(() -> {
            try {
                questionService.refreshQuestionData();
            } catch (Exception e) {
                log.warn("启动时同步题目索引失败，将在下次题目变更时重试: {}", e.getMessage());
            }
        });
    }
}
