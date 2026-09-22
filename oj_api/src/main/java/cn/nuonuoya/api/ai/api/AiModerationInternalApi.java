package cn.nuonuoya.api.ai.api;

import cn.nuonuoya.api.ai.dto.AiImageModerationDTO;
import cn.nuonuoya.api.ai.dto.AiTextModerationDTO;
import cn.nuonuoya.api.ai.vo.AiModerationVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// AI 内容审核内部接口（调用方：oj-friend 用户资料修改与头像上传；只给结论，拦截与否由调用方决定）
public interface AiModerationInternalApi {

    // 审核一组文本（任一条违规即不通过）
    @PostMapping("/ai/internal/moderation/text")
    AiModerationVO moderateText(@RequestBody AiTextModerationDTO moderationDTO);

    // 审核一张图片
    @PostMapping("/ai/internal/moderation/image")
    AiModerationVO moderateImage(@RequestBody AiImageModerationDTO moderationDTO);
}
