package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.dto.FriendAppealHandleDTO;
import cn.nuonuoya.api.friend.dto.FriendAppealQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendAppealQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendAppealVO;
import cn.nuonuoya.api.friend.vo.FriendPageVO;
import cn.nuonuoya.common.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

// C端申诉服务调用封装（列表、详情、裁定失败时抛出 3501；题目标记统计失败时降级为空，不影响题目列表）
@Slf4j
@Component
public class FriendAppealClient {

    @Autowired
    private FriendAppealFeignClient friendAppealFeignClient;

    // 分页查询申诉
    public FriendPageVO<FriendAppealVO> listAppeals(FriendAppealQueryDTO queryDTO) {
        return RemoteCallGuard.call("查询申诉", ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE,
                () -> friendAppealFeignClient.listAppeals(queryDTO),
                page -> page != null && page.getRows() != null);
    }

    // 查询申诉详情，不存在返回 null
    public FriendAppealDetailVO getAppeal(Long appealId) {
        return RemoteCallGuard.call("查询申诉详情", ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE,
                () -> friendAppealFeignClient.getAppeal(appealId),
                detail -> detail == null || detail.getAppealId() != null);
    }

    // 裁定申诉，申诉不存在或已是终态时返回 false
    public boolean handleAppeal(Long appealId, FriendAppealHandleDTO handleDTO) {
        return RemoteCallGuard.call("裁定申诉", ResultCode.FAILED_SUBMIT_SERVICE_UNAVAILABLE,
                () -> friendAppealFeignClient.handleAppeal(appealId, handleDTO),
                result -> result != null);
    }

    // 题目成立申诉统计，失败时返回空列表（只用于题目列表上的标记）
    public List<FriendAppealQuestionStatVO> upheldStats(List<Long> questionIds) {
        try {
            List<FriendAppealQuestionStatVO> stats = friendAppealFeignClient.upheldStats(questionIds);
            return stats == null ? Collections.emptyList() : stats;
        } catch (Exception e) {
            log.warn("查询题目成立申诉统计失败，题目列表不显示申诉标记, error = {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
