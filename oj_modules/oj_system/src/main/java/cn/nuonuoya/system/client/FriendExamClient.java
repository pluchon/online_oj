package cn.nuonuoya.system.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// C端竞赛服务调用封装（失败时返回 false，由调用方决定后续处理）
@Slf4j
@Component
public class FriendExamClient {

    @Autowired
    private FriendExamFeignClient friendExamFeignClient;

    // 通知C端刷新竞赛缓存（examId 为空时仅重建列表），成功返回 true
    public boolean refreshExamCache(Long examId) {
        try {
            friendExamFeignClient.refreshExamCache(examId);
            return true;
        } catch (Exception e) {
            log.error("通知C端刷新竞赛缓存失败, examId = {}, error = {}", examId, e.getMessage());
            return false;
        }
    }
}
