package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.dto.UserSubmitDTO;
import cn.nuonuoya.friend.vo.UserSubmitResultVO;

// 用户代码提交业务接口
public interface UserSubmitService {

    // 提交代码并异步投递判题消息
    UserSubmitResultVO submit(UserSubmitDTO submitDTO);

    // 根据提交ID查询当前评测状态与结果
    UserSubmitResultVO getSubmitResult(Long submitId);
}
