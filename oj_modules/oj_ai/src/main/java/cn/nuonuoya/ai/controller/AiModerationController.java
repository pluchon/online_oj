package cn.nuonuoya.ai.controller;

import cn.nuonuoya.ai.service.AiModerationService;
import cn.nuonuoya.api.ai.api.AiModerationInternalApi;
import cn.nuonuoya.api.ai.dto.AiImageModerationDTO;
import cn.nuonuoya.api.ai.dto.AiTextModerationDTO;
import cn.nuonuoya.api.ai.vo.AiModerationVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// AI 内容审核内部接口控制器（不经网关暴露）
@RestController
public class AiModerationController implements AiModerationInternalApi {

    @Autowired
    private AiModerationService aiModerationService;

    /** 审核文本 */
    @Override
    public AiModerationVO moderateText(@Valid @RequestBody AiTextModerationDTO moderationDTO) {
        return aiModerationService.moderateText(moderationDTO);
    }

    /** 审核图片 */
    @Override
    public AiModerationVO moderateImage(@Valid @RequestBody AiImageModerationDTO moderationDTO) {
        return aiModerationService.moderateImage(moderationDTO);
    }
}
