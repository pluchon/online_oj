package cn.nuonuoya.judge.controller;

import cn.nuonuoya.api.judge.api.JudgeInternalApi;
import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import cn.nuonuoya.judge.service.JudgeSandboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

// 判题服务内部接口控制器（不经网关暴露）
@RestController
public class JudgeInternalController implements JudgeInternalApi {

    // 注入沙箱判题执行服务
    @Autowired
    private JudgeSandboxService judgeSandboxService;

    /** 同步执行判题并返回逐用例结果 */
    @Override
    public JudgeResultVO run(JudgeRequestDTO requestDTO) {
        return judgeSandboxService.executeJudge(requestDTO);
    }
}
