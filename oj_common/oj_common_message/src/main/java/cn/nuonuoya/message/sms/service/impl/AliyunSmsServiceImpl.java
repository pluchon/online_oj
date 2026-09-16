package cn.nuonuoya.message.sms.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import cn.nuonuoya.message.sms.config.SmsProperties;
import cn.nuonuoya.message.sms.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

// 阿里云短信业务实现类
@Slf4j
@Service
public class AliyunSmsServiceImpl implements SmsService {

    @Autowired
    private SmsProperties smsProperties;

    // 阿里云号码认证客户端实例缓存
    private volatile Client client;

    // 发送默认模板短信验证码
    @Override
    public boolean sendCode(String phone, String code) {
        return sendCode(phone, code, smsProperties.getExpireMin() != null ? smsProperties.getExpireMin() : 5);
    }

    // 发送指定有效期的短信验证码
    @Override
    public boolean sendCode(String phone, String code, int expireMin) {
        String templateCode = smsProperties.getTemplateCode();
        JSONObject params = new JSONObject();
        params.put("code", code);
        // 速通互联等模板需要min有效期字段
        params.put("min", String.valueOf(expireMin));
        return sendMessage(phone, templateCode, params.toJSONString());
    }

    // 发送指定模板与参数Map的短信
    @Override
    public boolean sendMessage(String phone, String templateCode, Map<String, Object> paramMap) {
        String paramJson = (paramMap != null && !paramMap.isEmpty()) ? JSON.toJSONString(paramMap) : "{}";
        return sendMessage(phone, templateCode, paramJson);
    }

    // 发送指定模板与JSON参数字符串的短信
    @Override
    public boolean sendMessage(String phone, String templateCode, String templateParamJson) {
        if (!StringUtils.hasText(phone)) {
            log.warn("发送短信失败: 手机号为空");
            return false;
        }
        if (!StringUtils.hasText(templateCode)) {
            templateCode = smsProperties.getTemplateCode();
        }
        // 检查密钥是否配置，未配置时输出日志模拟放行，避免本地开发阻塞
        if (!StringUtils.hasText(smsProperties.getAccessKeyId()) || !StringUtils.hasText(smsProperties.getAccessKeySecret())) {
            log.warn("阿里云短信密钥未配置，本地跳过真实发送，接收手机号: {}, 模板Code: {}", maskPhone(phone), templateCode);
            return true;
        }

        try {
            Client smsClient = getClient();
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSignName(smsProperties.getSignName())
                    .setTemplateCode(templateCode)
                    .setPhoneNumber(phone)
                    .setTemplateParam(templateParamJson);
            RuntimeOptions runtime = new RuntimeOptions();
            SendSmsVerifyCodeResponse response = smsClient.sendSmsVerifyCodeWithOptions(request, runtime);

            if (response.getBody() != null && "OK".equalsIgnoreCase(response.getBody().getCode())) {
                log.info("向手机号 {} 发送短信成功, templateCode={}", maskPhone(phone), templateCode);
                return true;
            } else {
                String errorMsg = (response.getBody() != null) ? response.getBody().getMessage() : "响应体为空";
                log.error("向手机号 {} 发送短信失败, templateCode={}, 原因: {}", maskPhone(phone), templateCode, errorMsg);
                return false;
            }
        } catch (TeaException error) {
            log.error("调用阿里云短信SDK异常, 错误码: {}, 详情: {}", error.getCode(), error.getMessage());
            return false;
        } catch (Exception e) {
            log.error("调用短信发送服务发生非预期异常", e);
            return false;
        }
    }

    // 双重检查锁定初始化阿里云号码认证客户端
    private Client getClient() throws Exception {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    Config config = new Config()
                            .setAccessKeyId(smsProperties.getAccessKeyId())
                            .setAccessKeySecret(smsProperties.getAccessKeySecret());
                    config.endpoint = StringUtils.hasText(smsProperties.getEndpoint()) ? smsProperties.getEndpoint() : "dypnsapi.aliyuncs.com";
                    this.client = new Client(config);
                }
            }
        }
        return client;
    }

    // 手机号脱敏方法
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return "****";
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }
}
