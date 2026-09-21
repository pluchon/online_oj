package cn.nuonuoya.api.judge.api;

import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.api.judge.vo.JudgeResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// 判题服务内部接口契约（调用方：oj-friend 运行示例用例；提供方：oj-judge；只读不落库）
public interface JudgeInternalApi {

    // 同步执行判题并返回逐用例结果
    @PostMapping("/judge/internal/run")
    JudgeResultVO run(@RequestBody JudgeRequestDTO requestDTO);
}
