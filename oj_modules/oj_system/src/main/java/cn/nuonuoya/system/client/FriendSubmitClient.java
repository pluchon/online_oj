package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.dto.FriendSubmitQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.api.friend.vo.FriendRejudgeResultVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitDetailVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitPageVO;
import cn.nuonuoya.common.enums.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// C端提交记录服务调用封装（远程不可用或提供方出错时抛出 3501，由全局异常处理返回给管理端）
@Component
public class FriendSubmitClient {

    @Autowired
    private FriendSubmitFeignClient friendSubmitFeignClient;

    // 分页查询提交记录
    public FriendSubmitPageVO listSubmits(FriendSubmitQueryDTO queryDTO) {
        return RemoteCallGuard.call("查询提交记录", ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE,
                () -> friendSubmitFeignClient.listSubmits(queryDTO),
                page -> page != null && page.getRows() != null);
    }

    // 查询单条提交详情，不存在返回 null
    public FriendSubmitDetailVO getSubmit(Long submitId) {
        return RemoteCallGuard.call("查询提交详情", ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE,
                () -> friendSubmitFeignClient.getSubmit(submitId),
                detail -> detail == null || detail.getSubmitId() != null);
    }

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
