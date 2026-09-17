package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.ExamAddDTO;
import cn.nuonuoya.system.dto.ExamDTO;
import cn.nuonuoya.system.dto.ExamEditDTO;
import cn.nuonuoya.system.dto.ExamQuestionAddDTO;
import cn.nuonuoya.system.vo.ExamDetailVO;
import cn.nuonuoya.system.vo.ExamVO;
import cn.nuonuoya.system.vo.QuestionVO;

import java.util.List;

// 竞赛业务接口
public interface ExamService {

    // 分页查询竞赛列表
    List<ExamVO> list(ExamDTO queryDTO);

    // 新增竞赛基本信息并返回生成的竞赛ID
    String add(ExamAddDTO addDTO);

    // 获取竞赛详情
    ExamDetailVO getDetail(Long examId);

    // 编辑竞赛基本信息
    int edit(ExamEditDTO editDTO);

    // 删除竞赛
    int delete(Long examId);

    // 发布竞赛
    int publish(Long examId);

    // 撤销发布竞赛
    int cancelPublish(Long examId);

    // 绑定题目到竞赛
    int addQuestion(ExamQuestionAddDTO addDTO);

    // 查询竞赛关联的题目列表
    List<QuestionVO> getQuestionList(Long examId);

    // 从竞赛中移除指定题目
    int deleteQuestion(Long examId, Long questionId);

    // 同步预热所有已发布竞赛缓存
    void syncCache();
}
