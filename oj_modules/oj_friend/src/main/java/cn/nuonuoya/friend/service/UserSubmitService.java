package cn.nuonuoya.friend.service;

import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.dto.QuestionRunDTO;
import cn.nuonuoya.friend.dto.SubmitHistoryQueryDTO;
import cn.nuonuoya.friend.dto.UserSubmitDTO;
import cn.nuonuoya.friend.vo.QuestionRunResultVO;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;
import cn.nuonuoya.friend.vo.UserSubmitResultVO;

// 用户代码提交业务接口
public interface UserSubmitService {

    // 提交代码并异步投递判题消息
    UserSubmitResultVO submit(UserSubmitDTO submitDTO);

    // 同步运行公开示例用例（不落库、不计分）
    QuestionRunResultVO run(QuestionRunDTO runDTO);

    // 分页查询当前用户本题的提交记录
    TableDataResult<SubmitHistoryVO> listHistory(SubmitHistoryQueryDTO queryDTO);

    // 根据提交ID查询当前评测状态与结果
    UserSubmitResultVO getSubmitResult(Long submitId);

    // 回写异步判题结果
    void saveJudgeResult(JudgeResultVO resultVO);
}
