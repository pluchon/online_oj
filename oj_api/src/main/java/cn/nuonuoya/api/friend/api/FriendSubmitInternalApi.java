package cn.nuonuoya.api.friend.api;

import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.api.friend.vo.FriendRejudgeResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// C端提交记录内部接口契约（提供方：oj-friend；调用方：oj-system 修改题目用例后按题重判。预览只读；重判为写操作，把提交重新投递到判题队列）
public interface FriendSubmitInternalApi {

    // 预览按题重判的影响范围（只读）
    @GetMapping("/friend/internal/submit/rejudge/preview")
    FriendRejudgePreviewVO previewRejudge(@RequestParam("questionId") Long questionId);

    // 按题重判：练习提交与未结算竞赛的提交重新投递判题，已在评测中的跳过；队列投递失败时停止并在结果中标明
    @PostMapping("/friend/internal/submit/rejudge")
    FriendRejudgeResultVO rejudge(@RequestParam("questionId") Long questionId);
}
