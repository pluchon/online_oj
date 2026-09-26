package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.dto.FriendSubmitQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.api.friend.vo.FriendRejudgeResultVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitDetailVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitPageVO;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;
import java.util.function.Supplier;

// C端提交记录服务调用封装（远程不可用或提供方出错时抛出 3501，由全局异常处理返回给管理端）
@Slf4j
@Component
public class FriendSubmitClient {

    @Autowired
    private FriendSubmitFeignClient friendSubmitFeignClient;

    // 分页查询提交记录
    public FriendSubmitPageVO listSubmits(FriendSubmitQueryDTO queryDTO) {
        return call("查询提交记录", () -> friendSubmitFeignClient.listSubmits(queryDTO),
                page -> page != null && page.getRows() != null);
    }

    // 查询单条提交详情，不存在返回 null
    public FriendSubmitDetailVO getSubmit(Long submitId) {
        return call("查询提交详情", () -> friendSubmitFeignClient.getSubmit(submitId),
                detail -> detail == null || detail.getSubmitId() != null);
    }

    // 预览按题重判的影响范围
    public FriendRejudgePreviewVO previewRejudge(Long questionId) {
        return call("预览重判范围", () -> friendSubmitFeignClient.previewRejudge(questionId),
                preview -> preview != null && preview.getRejudgeCount() != null);
    }

    // 按题重判
    public FriendRejudgeResultVO rejudge(Long questionId) {
        return call("按题重判", () -> friendSubmitFeignClient.rejudge(questionId),
                result -> result != null && result.getQueuedCount() != null);
    }

    // 统一远程调用：远程异常，或关键字段为空的结果都视为不可用
    // （提供方出错时全局异常处理仍以 200 返回错误码，Feign 会把它解析成字段全空的对象，不能当作正常结果）
    private <T> T call(String action, Supplier<T> supplier, Predicate<T> valid) {
        T result;
        try {
            result = supplier.get();
        } catch (Exception e) {
            log.error("{}失败, error = {}", action, e.getMessage());
            throw new ServiceException(ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE);
        }
        if (!valid.test(result)) {
            log.error("{}失败：提供方返回了错误响应", action);
            throw new ServiceException(ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE);
        }
        return result;
    }
}
