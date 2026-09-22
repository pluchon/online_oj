package cn.nuonuoya.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.ai.config.AiChatOptionsFactory;
import cn.nuonuoya.ai.config.AiProperties;
import cn.nuonuoya.ai.exception.AiModelException;
import cn.nuonuoya.ai.prompt.ModerationPrompts;
import cn.nuonuoya.ai.service.AiModerationService;
import cn.nuonuoya.api.ai.dto.AiImageModerationDTO;
import cn.nuonuoya.api.ai.dto.AiTextModerationDTO;
import cn.nuonuoya.api.ai.vo.AiModerationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.function.Supplier;

// 内容审核实现：低温度、结构化输出，只给结论不做拦截
@Slf4j
@Service
public class AiModerationServiceImpl implements AiModerationService {

    // 审核使用的采样温度
    private static final double MODERATION_TEMPERATURE = 0.0;

    @Autowired
    private ChatClient moderationChatClient;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private AiChatOptionsFactory aiChatOptionsFactory;

    // 审核文本
    @Override
    public AiModerationVO moderateText(AiTextModerationDTO moderationDTO) {
        String joined = String.join("\n###\n", moderationDTO.getTexts());
        String model = aiProperties.getModerationModel();
        return call("文本审核", model, () -> moderationChatClient.prompt()
                .options(aiChatOptionsFactory.builder(model, MODERATION_TEMPERATURE).build())
                .system(ModerationPrompts.TEXT_SYSTEM)
                .user(joined)
                .call()
                .entity(AiModerationVO.class));
    }

    // 审核图片（以 data URL 形式随消息发送给视觉模型）
    @Override
    public AiModerationVO moderateImage(AiImageModerationDTO moderationDTO) {
        String model = aiProperties.getImageModerationModel();
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType(moderationDTO.getMimeType()))
                .data(new ByteArrayResource(moderationDTO.getData()))
                .build();
        UserMessage message = UserMessage.builder().text(ModerationPrompts.IMAGE_USER).media(media).build();
        return call("图片审核", model, () -> moderationChatClient.prompt()
                .options(aiChatOptionsFactory.builder(model, MODERATION_TEMPERATURE).multiModel(true).build())
                .system(ModerationPrompts.IMAGE_SYSTEM)
                .messages(message)
                .call()
                .entity(AiModerationVO.class));
    }

    // 调用模型并规整结论，任何失败都转换为模型调用异常
    private AiModerationVO call(String scene, String model, Supplier<AiModerationVO> action) {
        long start = System.currentTimeMillis();
        AiModerationVO result;
        try {
            result = action.get();
        } catch (Exception e) {
            log.warn("AI {}失败, model = {}, error = {}", scene, model, e.getMessage());
            throw new AiModelException(scene + "失败", e);
        }
        if (result == null || result.getPass() == null) {
            throw new AiModelException(scene + "：模型未给出结论");
        }
        if (result.getPass()) {
            result.setCategory(null);
        } else {
            result.setCategory(StrUtil.blankToDefault(StrUtil.trim(result.getCategory()), "违规内容"));
        }
        log.info("AI {}完成, model = {}, pass = {}, 耗时 = {} ms", scene, model, result.getPass(), System.currentTimeMillis() - start);
        return result;
    }
}
