package cn.nuonuoya.system.converter;

import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.enums.ExamStatus;
import cn.nuonuoya.system.vo.ExamDetailVO;
import cn.nuonuoya.system.vo.ExamVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 竞赛对象转换器
public class ExamConverter {

    // 实体对象转换为列表VO对象
    public static ExamVO toVO(TbExam entity, String creatorName) {
        if (entity == null) {
            return null;
        }
        ExamVO vo = new ExamVO();
        vo.setExamId(entity.getExamId());
        vo.setTitle(entity.getTitle());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(ExamStatus.getDescByValue(entity.getStatus()));
        vo.setCreatorName(creatorName);
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    // 实体对象转换为详情VO对象
    public static ExamDetailVO toDetailVO(TbExam entity, String creatorName) {
        if (entity == null) {
            return null;
        }
        ExamDetailVO vo = new ExamDetailVO();
        vo.setExamId(entity.getExamId());
        vo.setTitle(entity.getTitle());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(ExamStatus.getDescByValue(entity.getStatus()));
        vo.setCreatorName(creatorName);
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
