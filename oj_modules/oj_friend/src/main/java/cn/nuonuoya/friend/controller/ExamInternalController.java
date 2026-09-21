package cn.nuonuoya.friend.controller;

import cn.nuonuoya.api.friend.api.FriendExamInternalApi;
import cn.nuonuoya.friend.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

// C端竞赛内部接口控制器（网关已屏蔽 internal 路径，仅供服务间调用）
@RestController
public class ExamInternalController implements FriendExamInternalApi {

    @Autowired
    private ExamService examService;

    /** 刷新竞赛缓存 */
    @Override
    public Integer refreshExamCache(Long examId) {
        return examService.refreshExamCache(examId);
    }

    /** 结算已结束且未结算的竞赛 */
    @Override
    public Integer settleFinishedExams() {
        return examService.settleFinishedExams();
    }
}
