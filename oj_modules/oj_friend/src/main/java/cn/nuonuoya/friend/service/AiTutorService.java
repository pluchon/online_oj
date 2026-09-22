package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.dto.AiTutorAskDTO;
import cn.nuonuoya.friend.vo.AiTutorSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// AI 做题辅导业务
public interface AiTutorService {

    // 查询当前用户在本题的会话与快捷操作状态
    AiTutorSessionVO getSession(Long questionId);

    // 提问：校验资格与次数后以 SSE 流式返回回复，完成后落库
    SseEmitter ask(Long questionId, AiTutorAskDTO askDTO);
}
