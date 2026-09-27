package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.vo.ExamReviewVO;

import java.util.Collection;
import java.util.Set;

// 赛后复盘业务接口（数字由 SQL 统计，AI 只写点评；学员第一次打开时生成，每场可手动重新生成 3 次）
public interface ExamReviewService {

    // 读取本人这场的复盘：已生成且提交结果没变时返回，否则返回 null
    ExamReviewVO getReview(Long examId);

    // 生成本人这场的复盘：已有且仍有效时直接返回，否则统计并调用 AI 后保存
    ExamReviewVO generateReview(Long examId);

    // 手动重新生成本人这场的复盘（每场最多 3 次；还没生成过时等同于生成，不计次数）
    ExamReviewVO regenerateReview(Long examId);

    // 在给定竞赛中筛出本人可以看复盘的（已结束且已结算、本人有提交）
    Set<Long> listReviewableExamIds(Long userId, Collection<Long> examIds);
}
