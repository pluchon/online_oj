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

// C端提交记录服务调用封装（远程不可用时抛出 3501，由全局异常处理返回给管理端）
@Slf4j
@Component
public class FriendSubmitClient {

    @Autowired
    private FriendSubmitFeignClient friendSubmitFeignClient;

    // 分页查询提交记录
    public FriendSubmitPageVO listSubmits(FriendSubmitQueryDTO queryDTO) {
        try {
            return friendSubmitFeignClient.listSubmits(queryDTO);
        } catch (Exception e) {
            log.error("查询提交记录失败, error = {}", e.getMessage());
            throw new ServiceException(ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE);
        }
    }

    // 查询单条提交详情，不存在返回 null
    public FriendSubmitDetailVO getSubmit(Long submitId) {
        try {
            return friendSubmitFeignClient.getSubmit(submitId);
        } catch (Exception e) {
            log.error("查询提交详情失败, submitId = {}, error = {}", submitId, e.getMessage());
            throw new ServiceException(ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE);
        }
    }

    // 预览按题重判的影响范围
    public FriendRejudgePreviewVO previewRejudge(Long questionId) {
        try {
            return friendSubmitFeignClient.previewRejudge(questionId);
        } catch (Exception e) {
            log.error("预览重判范围失败, questionId = {}, error = {}", questionId, e.getMessage());
            throw new ServiceException(ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE);
        }
    }

    // 按题重判
    public FriendRejudgeResultVO rejudge(Long questionId) {
        try {
            return friendSubmitFeignClient.rejudge(questionId);
        } catch (Exception e) {
            log.error("按题重判失败, questionId = {}, error = {}", questionId, e.getMessage());
            throw new ServiceException(ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE);
        }
    }
}
