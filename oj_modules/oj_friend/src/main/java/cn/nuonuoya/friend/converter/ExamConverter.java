package cn.nuonuoya.friend.converter;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.domain.TbUserExam;
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

        // 依据当前系统时间与起止时间动态判断竞赛状态
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            vo.setContestStatus(0);
            vo.setContestStatusDesc("未开赛");
            vo.setBtnText("报名参赛");
        } else if (exam.getEndTime() != null && !now.isAfter(exam.getEndTime())) {
            vo.setContestStatus(1);
            vo.setContestStatusDesc("进行中");
            vo.setBtnText("进入竞赛");
        } else {
            vo.setContestStatus(2);
            vo.setContestStatusDesc("已完赛");
            vo.setBtnText("已完赛");
        }

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
    public static UserExamVO toUserExamVO(TbExam exam, cn.nuonuoya.friend.domain.TbUserExam userExam) {
        if (exam == null) {
            return null;
        }
        cn.nuonuoya.friend.vo.UserExamVO vo = new cn.nuonuoya.friend.vo.UserExamVO();
        vo.setExamId(exam.getExamId());
        vo.setTitle(exam.getTitle());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        if (userExam != null) {
            vo.setScore(userExam.getScore());
            vo.setExamRank(userExam.getExamRank());
            vo.setCreateTime(userExam.getCreateTime());
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            vo.setContestStatus(0);
            vo.setContestStatusDesc("未开赛");
        } else if (exam.getEndTime() != null && !now.isAfter(exam.getEndTime())) {
            vo.setContestStatus(1);
            vo.setContestStatusDesc("进行中");
        } else {
            vo.setContestStatus(2);
            vo.setContestStatusDesc("已完赛");
        }
        vo.setIsEnter(true);
        return vo;
    }
}
