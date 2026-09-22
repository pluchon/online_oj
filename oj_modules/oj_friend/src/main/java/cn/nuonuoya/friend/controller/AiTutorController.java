package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.friend.aspect.CheckUserStatus;
import cn.nuonuoya.friend.dto.AiTutorAskDTO;
import cn.nuonuoya.friend.service.AiTutorService;
import cn.nuonuoya.friend.vo.AiTutorSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// AI 做题辅导控制器
@Validated
@RestController
@RequestMapping("/ai/tutor")
@Tag(name = "AI 做题辅导API")
public class AiTutorController extends BaseController {

    @Autowired
    private AiTutorService aiTutorService;

    /** 查询本题的辅导会话与剩余次数 */
    @GetMapping("/{questionId}")
    @Operation(summary = "辅导会话", description = "历史消息、今日剩余次数与快捷操作所需的提交状态")
    public OJResult<AiTutorSessionVO> session(@PathVariable("questionId") Long questionId,
                                              @RequestParam(value = "examId", required = false) Long examId) {
        return OJResult.ok(aiTutorService.getSession(questionId, examId));
    }

    /** 提问并流式返回回复 */
    @CheckUserStatus
    @PostMapping("/{questionId}/chat")
    @Operation(summary = "提问", description = "SSE 流式返回：delta 增量文本、done 结束、error 失败；校验失败时直接返回 JSON 错误（不声明 produces，保证错误能以 JSON 返回）")
    public SseEmitter chat(@PathVariable("questionId") Long questionId, @Validated @RequestBody AiTutorAskDTO askDTO) {
        return aiTutorService.ask(questionId, askDTO);
    }
}
