package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.api.friend.vo.FriendRejudgeResultVO;
import cn.nuonuoya.common.enums.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// C端按题重判调用封装（远程不可用或提供方出错时抛出 3501，由全局异常处理返回给管理端）
@Component
public class FriendSubmitClient {

    @Autowired
    private FriendSubmitFeignClient friendSubmitFeignClient;

    // 预览按题重判的影响范围
    public FriendRejudgePreviewVO previewRejudge(Long questionId) {
        return RemoteCallGuard.call("预览重判范围", ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE,
                () -> friendSubmitFeignClient.previewRejudge(questionId),
                preview -> preview != null && preview.getRejudgeCount() != null);
    }

    // 按题重判
    public FriendRejudgeResultVO rejudge(Long questionId) {
        return RemoteCallGuard.call("按题重判", ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE,
                () -> friendSubmitFeignClient.rejudge(questionId),
                result -> result != null && result.getQueuedCount() != null);
    }
}
