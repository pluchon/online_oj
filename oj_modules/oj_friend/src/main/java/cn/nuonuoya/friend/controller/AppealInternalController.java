package cn.nuonuoya.friend.controller;

import cn.nuonuoya.api.friend.api.FriendAppealInternalApi;
import cn.nuonuoya.api.friend.dto.FriendAppealHandleDTO;
import cn.nuonuoya.api.friend.dto.FriendAppealQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendAppealQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendAppealVO;
import cn.nuonuoya.api.friend.vo.FriendPageVO;
import cn.nuonuoya.friend.service.AppealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// C端申诉内部接口控制器（网关已屏蔽 internal 路径，仅供服务间调用）
@RestController
public class AppealInternalController implements FriendAppealInternalApi {

    @Autowired
    private AppealService appealService;

    /** 分页查询申诉 */
    @Override
    public FriendPageVO<FriendAppealVO> listAppeals(@RequestBody FriendAppealQueryDTO queryDTO) {
        return appealService.listForManage(queryDTO);
    }

    /** 查询申诉详情 */
    @Override
    public FriendAppealDetailVO getAppeal(@PathVariable("appealId") Long appealId) {
        return appealService.getForManage(appealId);
    }

    /** 裁定申诉 */
    @Override
    public Boolean handleAppeal(@PathVariable("appealId") Long appealId, @RequestBody FriendAppealHandleDTO handleDTO) {
        return appealService.handle(appealId, handleDTO);
    }

    /** 题目成立申诉统计 */
    @Override
    public List<FriendAppealQuestionStatVO> upheldStats(@RequestBody List<Long> questionIds) {
        return appealService.upheldStats(questionIds);
    }
}
