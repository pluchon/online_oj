package cn.nuonuoya.friend.client;

import cn.nuonuoya.api.ai.dto.AiImageModerationDTO;
import cn.nuonuoya.api.ai.dto.AiTextModerationDTO;
import cn.nuonuoya.api.ai.vo.AiModerationVO;
import cn.nuonuoya.friend.constants.SentinelResources;
import cn.nuonuoya.sentinel.SentinelGuard;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

// AI 审核调用边界：审核服务不可用、超时、被限流或熔断时返回 null（调用方放行并记录告警），不伪造审核结论
@Slf4j
@Component
public class AiModerationClient {

    @Autowired
    private AiModerationFeignClient aiModerationFeignClient;

    // 审核文本，服务不可用时返回 null
    public AiModerationVO moderateText(List<String> texts) {
        AiTextModerationDTO dto = new AiTextModerationDTO();
        dto.setTexts(texts);
        try {
            return SentinelGuard.call(SentinelResources.AI_MODERATION, () -> aiModerationFeignClient.moderateText(dto));
        } catch (BlockException e) {
            log.warn("AI 文本审核被限流或熔断，已放行, rule = {}", e.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.warn("AI 文本审核不可用，已放行, error = {}", e.getMessage());
            return null;
        }
    }

    // 审核图片，服务不可用时返回 null
    public AiModerationVO moderateImage(String mimeType, byte[] data) {
        AiImageModerationDTO dto = new AiImageModerationDTO();
        dto.setMimeType(mimeType);
        dto.setData(data);
        try {
            return SentinelGuard.call(SentinelResources.AI_MODERATION, () -> aiModerationFeignClient.moderateImage(dto));
        } catch (BlockException e) {
            log.warn("AI 图片审核被限流或熔断，已放行, rule = {}", e.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.warn("AI 图片审核不可用，已放行, error = {}", e.getMessage());
            return null;
        }
    }
}
