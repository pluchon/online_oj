package cn.nuonuoya.job.handler;

import cn.nuonuoya.job.client.FriendExamFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 竞赛定时任务处理器（调用 C 端竞赛内部接口，失败时标记任务失败由调度中心重试）
@Slf4j
@Component
public class ExamJobHandler {

    @Autowired
    private FriendExamFeignClient friendExamFeignClient;

    // 定时重建未完赛与历史竞赛列表缓存（竞赛随时间从未完赛转入历史）
    @XxlJob("examListOrganizeHandler")
    public void examListOrganizeHandler() {
        try {
            Integer total = friendExamFeignClient.refreshExamCache(null);
            XxlJobHelper.handleSuccess("竞赛列表缓存已重建，共 " + total + " 场");
        } catch (Exception e) {
            log.error("竞赛列表缓存重建失败", e);
            XxlJobHelper.handleFail("竞赛列表缓存重建失败: " + e.getMessage());
        }
    }

    // 定时结算已结束且未结算的竞赛（排名落库并发送战报，重复执行不会重复发送）
    @XxlJob("examRankSettlementHandler")
    public void examRankSettlementHandler() {
        try {
            Integer settled = friendExamFeignClient.settleFinishedExams();
            XxlJobHelper.handleSuccess("本次结算竞赛 " + settled + " 场");
        } catch (Exception e) {
            log.error("竞赛排名结算失败", e);
            XxlJobHelper.handleFail("竞赛排名结算失败: " + e.getMessage());
        }
    }
}
