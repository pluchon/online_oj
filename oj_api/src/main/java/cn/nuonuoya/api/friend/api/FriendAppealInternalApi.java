package cn.nuonuoya.api.friend.api;

import cn.nuonuoya.api.friend.dto.FriendAppealHandleDTO;
import cn.nuonuoya.api.friend.dto.FriendAppealQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendAppealQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendAppealVO;
import cn.nuonuoya.api.friend.vo.FriendPageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// C端申诉内部接口契约（提供方：oj-friend，申诉与提交数据归它；调用方：oj-system 申诉管理。列表、详情、统计只读；裁定为写操作）
public interface FriendAppealInternalApi {

    // 按条件分页查询申诉（按申诉时间倒序）
    @PostMapping("/friend/internal/appeal/list")
    FriendPageVO<FriendAppealVO> listAppeals(@RequestBody FriendAppealQueryDTO queryDTO);

    // 查询申诉详情（含提交代码与逐用例结果），不存在时返回空
    @GetMapping("/friend/internal/appeal/{appealId}")
    FriendAppealDetailVO getAppeal(@PathVariable("appealId") Long appealId);

    // 裁定申诉（已裁定的也可改判）：通过即改判为满分，撤回通过则恢复原结论，并通知学员；申诉不存在或刚被他人改动时返回 false
    @PostMapping("/friend/internal/appeal/{appealId}/handle")
    Boolean handleAppeal(@PathVariable("appealId") Long appealId, @RequestBody FriendAppealHandleDTO handleDTO);

    // 统计指定题目中裁定为通过的申诉数与最近裁定时间（没有的题不返回）
    @PostMapping("/friend/internal/appeal/upheld-stats")
    List<FriendAppealQuestionStatVO> upheldStats(@RequestBody List<Long> questionIds);
}
