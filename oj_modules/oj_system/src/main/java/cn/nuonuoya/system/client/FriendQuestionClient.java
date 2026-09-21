package cn.nuonuoya.system.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// C端题目服务调用封装（失败时返回 false，由调用方决定后续处理）
@Slf4j
@Component
public class FriendQuestionClient {

    @Autowired
    private FriendQuestionFeignClient friendQuestionFeignClient;

    // 通知C端刷新题目缓存与ES索引，成功返回 true
    public boolean refreshQuestionData() {
        try {
            friendQuestionFeignClient.refreshQuestionData();
            return true;
        } catch (Exception e) {
            log.error("通知C端刷新题目数据失败, error = {}", e.getMessage());
            return false;
        }
    }
}
