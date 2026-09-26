package cn.nuonuoya.friend.controller;

import cn.nuonuoya.api.friend.api.FriendSubmitInternalApi;
import cn.nuonuoya.api.friend.dto.FriendSubmitQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.api.friend.vo.FriendRejudgeResultVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitDetailVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitPageVO;
import cn.nuonuoya.friend.service.UserSubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// C端提交记录内部接口控制器（网关已屏蔽 internal 路径，仅供服务间调用）
@RestController
public class SubmitInternalController implements FriendSubmitInternalApi {

    @Autowired
    private UserSubmitService userSubmitService;

    /** 分页查询提交记录 */
    @Override
    public FriendSubmitPageVO listSubmits(@RequestBody FriendSubmitQueryDTO queryDTO) {
        return userSubmitService.listForManage(queryDTO);
    }

    /** 查询单条提交详情 */
    @Override
    public FriendSubmitDetailVO getSubmit(@PathVariable("submitId") Long submitId) {
        return userSubmitService.getForManage(submitId);
    }

    /** 预览按题重判的影响范围 */
    @Override
    public FriendRejudgePreviewVO previewRejudge(@RequestParam("questionId") Long questionId) {
        return userSubmitService.previewRejudge(questionId);
    }

    /** 按题重判 */
    @Override
    public FriendRejudgeResultVO rejudge(@RequestParam("questionId") Long questionId) {
        return userSubmitService.rejudge(questionId);
    }
}
