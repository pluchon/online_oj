package cn.nuonuoya.ai.service;

import cn.nuonuoya.api.ai.dto.AiImageModerationDTO;
import cn.nuonuoya.api.ai.dto.AiTextModerationDTO;
import cn.nuonuoya.api.ai.vo.AiModerationVO;

// 内容审核 AI 能力
public interface AiModerationService {

    // 审核文本
    AiModerationVO moderateText(AiTextModerationDTO moderationDTO);

    // 审核图片
    AiModerationVO moderateImage(AiImageModerationDTO moderationDTO);
}
