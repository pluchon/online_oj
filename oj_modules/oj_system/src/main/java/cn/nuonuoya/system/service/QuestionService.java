package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.QuestionAddDTO;
import cn.nuonuoya.system.dto.QuestionDTO;
import cn.nuonuoya.system.dto.QuestionEditDTO;
import cn.nuonuoya.system.vo.QuestionDetailVO;
import cn.nuonuoya.system.vo.QuestionVO;

import java.util.List;

// 题目业务接口
public interface QuestionService {

    // 分页查询题目列表
    List<QuestionVO> list(QuestionDTO queryDTO);

    // 新增题目
    int add(QuestionAddDTO addDTO);

    // 获取题目详情
    QuestionDetailVO getDetail(Long questionId);

    // 修改题目
    int edit(QuestionEditDTO editDTO);

    // 删除题目
    int delete(Long questionId);
}
