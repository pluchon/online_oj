package cn.nuonuoya.friend.converter;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.enums.ExamContestStatusEnum;
import cn.nuonuoya.friend.vo.ExamVO;
import cn.nuonuoya.friend.vo.UserExamVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 竞赛实体转换器
public class ExamConverter {

    // 实体转换为视图对象并动态计算开赛状态
    public static ExamVO toVO(TbExam exam) {
        if (exam == null) {
            return null;
        }
        ExamVO vo = new ExamVO();
        vo.setExamId(exam.getExamId());
        vo.setTitle(exam.getTitle());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setStatus(exam.getStatus());

        ExamContestStatusEnum contestStatus = resolveContestStatus(exam);
        vo.setContestStatus(contestStatus.getCode());
        vo.setContestStatusDesc(contestStatus.getDesc());
        vo.setBtnText(contestStatus.getBtnText());
        return vo;
    }

    // 批量实体转换为视图对象列表
    public static List<ExamVO> toVOList(List<TbExam> examList) {
        if (CollUtil.isEmpty(examList)) {
            return Collections.emptyList();
        }
        List<ExamVO> voList = new ArrayList<>(examList.size());
        for (TbExam exam : examList) {
            voList.add(toVO(exam));
        }
        return voList;
    }

    // 组装用户已报名竞赛视图对象
    public static UserExamVO toUserExamVO(TbExam exam, TbUserExam userExam) {
        if (exam == null) {
            return null;
        }
        UserExamVO vo = new UserExamVO();
        vo.setExamId(exam.getExamId());
        vo.setTitle(exam.getTitle());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        if (userExam != null) {
            vo.setScore(userExam.getScore());
            vo.setExamRank(userExam.getExamRank());
            vo.setCreateTime(userExam.getCreateTime());
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
