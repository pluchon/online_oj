package cn.nuonuoya.friend.service;

import cn.nuonuoya.api.friend.dto.FriendAppealHandleDTO;
import cn.nuonuoya.api.friend.dto.FriendAppealQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendAppealQuestionStatVO;
import cn.nuonuoya.api.friend.vo.FriendAppealVO;
import cn.nuonuoya.api.friend.vo.FriendPageVO;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.AppealCreateDTO;
import cn.nuonuoya.friend.vo.AppealQuotaVO;
import cn.nuonuoya.friend.vo.AppealReviewVO;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;

import java.util.List;

// 提交申诉业务接口（AI 初审放行后才能正式申诉）
public interface AppealService {

    // 当前用户今日的初审与申诉剩余次数
    AppealQuotaVO getQuota();

    // 对当前用户的一条未通过提交发起 AI 初审
    AppealReviewVO review(Long submitId);

    // 初审放行后提交正式申诉
    void create(AppealCreateDTO createDTO);

    // 给本题提交记录补申诉状态与是否可申诉
    void fillHistoryAppeal(List<SubmitHistoryVO> voList, List<TbUserSubmit> submits);

    // 管理端按条件分页查询申诉
    FriendPageVO<FriendAppealVO> listForManage(FriendAppealQueryDTO queryDTO);

    // 管理端查询申诉详情，不存在返回 null
    FriendAppealDetailVO getForManage(Long appealId);

    // 管理端裁定申诉（已裁定的也可改判），申诉不存在或刚被他人改动时返回 false
    boolean handle(Long appealId, FriendAppealHandleDTO handleDTO);

    // 指定题目中裁定为通过的申诉统计
    List<FriendAppealQuestionStatVO> upheldStats(List<Long> questionIds);
}
