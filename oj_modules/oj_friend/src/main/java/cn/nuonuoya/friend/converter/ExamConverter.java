package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.enums.ExamContestStatusEnum;
import cn.nuonuoya.friend.vo.ExamVO;
import cn.nuonuoya.friend.vo.UserExamVO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

// 竞赛实体转换器
public class ExamConverter {

    // 实体转换为视图对象并动态计算开赛状态
    public static ExamVO toVO(TbExam exam) {
        if (exam == null) {
            return null;
        }
        ExamVO vo = BeanUtil.copyProperties(exam, ExamVO.class);

        ExamContestStatusEnum contestStatus = resolveContestStatus(exam);
        vo.setContestStatus(contestStatus.getCode());
        vo.setContestStatusDesc(contestStatus.getDesc());
        vo.setBtnText(contestStatus.getBtnText());
        return vo;
    }

    // 批量实体转换为视图对象列表
    public static List<ExamVO> toVOList(List<TbExam> examList) {
        return CollUtil.isEmpty(examList) ? Collections.emptyList() : examList.stream().map(ExamConverter::toVO).toList();
    }

    // 组装用户已报名竞赛视图对象（createTime 是报名时间，取自报名记录而不是竞赛）
    public static UserExamVO toUserExamVO(TbExam exam, TbUserExam userExam) {
        if (exam == null) {
            return null;
        }
        UserExamVO vo = BeanUtil.copyProperties(exam, UserExamVO.class, "createTime");
        if (userExam != null) {
            BeanUtil.copyProperties(userExam, vo, "examId");
        }

        ExamContestStatusEnum contestStatus = resolveContestStatus(exam);
        vo.setContestStatus(contestStatus.getCode());
        vo.setContestStatusDesc(contestStatus.getDesc());
        vo.setIsEnter(true);
        return vo;
    }

    // 依据当前系统时间与起止时间计算竞赛状态
    private static ExamContestStatusEnum resolveContestStatus(TbExam exam) {
        return ExamContestStatusEnum.of(exam.getStartTime(), exam.getEndTime(), LocalDateTime.now());
    }
}
