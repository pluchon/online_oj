package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.ExamAiPlanDTO;
import cn.nuonuoya.system.vo.ExamAiPlanVO;

// AI 帮建竞赛
public interface ExamAiService {

    // 生成竞赛名称与题目（只回填表单，不落库）
    ExamAiPlanVO plan(ExamAiPlanDTO planDTO);
}
