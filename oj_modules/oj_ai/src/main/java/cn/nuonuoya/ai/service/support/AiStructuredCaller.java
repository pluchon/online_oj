package cn.nuonuoya.ai.service.support;

import cn.nuonuoya.ai.config.AiChatOptionsFactory;
import cn.nuonuoya.ai.exception.AiModelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 结构化输出调用：以指定模型与温度调用并解析为对象，任何失败都转换为模型调用异常
@Slf4j
@Component
public class AiStructuredCaller {

    @Autowired
    private ChatClient questionChatClient;

    @Autowired
    private AiChatOptionsFactory aiChatOptionsFactory;

    // 调用模型并解析为指定类型
    public <T> T call(String scene, String model, Double temperature, String system, String user, Class<T> type) {
        long start = System.currentTimeMillis();
        try {
            T entity = questionChatClient.prompt()
                    .options(aiChatOptionsFactory.builder(model, temperature).build())
                    .system(system)
                    .user(user)
                    .call()
                    .entity(type);
            if (entity == null) {
                throw new AiModelException(scene + "：模型返回为空");
            }
            log.info("AI {}完成, model = {}, 耗时 = {} ms", scene, model, System.currentTimeMillis() - start);
            return entity;
        } catch (AiModelException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI {}失败, model = {}, 耗时 = {} ms, error = {}", scene, model,
                    System.currentTimeMillis() - start, e.getMessage());
            throw new AiModelException(scene + "：模型调用失败", e);
        }
    }
}
