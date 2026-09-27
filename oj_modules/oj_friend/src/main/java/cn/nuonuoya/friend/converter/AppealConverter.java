package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.ai.dto.AiAppealCaseDTO;
import cn.nuonuoya.api.ai.dto.AiAppealReviewDTO;
import cn.nuonuoya.api.friend.enums.AppealStatusEnum;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendCaseResultVO;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbSubmitAppeal;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import com.alibaba.fastjson2.JSON;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

// 申诉对象转换器
public class AppealConverter {

    private AppealConverter() {
    }

    // 申诉与被申诉的提交合并为详情（提交时间字段名不同，单独赋值）
    public static FriendAppealDetailVO toDetailVO(TbSubmitAppeal appeal, TbUserSubmit submit) {
        FriendAppealDetailVO vo = BeanUtil.copyProperties(appeal, FriendAppealDetailVO.class);
        if (submit != null) {
            BeanUtil.copyProperties(submit, vo, "createTime", "createBy", "updateTime", "updateBy", "examId", "questionId", "userId", "submitId");
            vo.setSubmitTime(submit.getCreateTime());
            vo.setCaseResults(parseCaseResults(submit.getCaseOutputs()));
        }
        return vo;
    }

    // 提交转换为新申诉（提交、用户、题目、竞赛 ID 同名复制；原结论字段名不同，单独赋值）
    public static TbSubmitAppeal toEntity(TbUserSubmit submit, String reason, String aiAnalysis, Long userId) {
        TbSubmitAppeal appeal = BeanUtil.copyProperties(submit, TbSubmitAppeal.class, "createTime", "createBy", "updateTime", "updateBy");
        appeal.setUserId(userId);
        appeal.setReason(reason);
        appeal.setAiAnalysis(aiAnalysis);
        appeal.setOriginJudgeStatus(submit.getJudgeStatus());
        appeal.setStatus(AppealStatusEnum.PENDING.getCode());
        appeal.setCreateBy(userId);
        appeal.setCreateTime(LocalDateTime.now());
        return appeal;
    }

    // 组装 AI 初审请求：题面，提交的代码、通过数与回显同名复制，其余由调用方算好传入
    public static AiAppealReviewDTO toReviewRequest(TbQuestion question, TbUserSubmit submit, String editorial,
                                                    String verdict, List<AiAppealCaseDTO> failedCases) {
        AiAppealReviewDTO dto = AiQuestionConverter.fillQuestion(question, BeanUtil.copyProperties(submit, AiAppealReviewDTO.class));
        dto.setEditorial(editorial);
        dto.setVerdict(verdict);
        dto.setFailedCases(failedCases);
        return dto;
    }

    // 解析提交表里的逐用例结果 JSON，没有记录时返回空列表
    public static List<FriendCaseResultVO> parseCaseResults(String caseOutputs) {
        return StrUtil.isBlank(caseOutputs) ? Collections.emptyList() : JSON.parseArray(caseOutputs, FriendCaseResultVO.class);
    }
}
