package cn.nuonuoya.message.sms.service.impl;

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
import org.springframework.util.StringUtils;

// 阿里云号码认证服务短信实现
@Slf4j
public class AliyunSmsServiceImpl implements SmsService {

    // 阿里云接口成功响应码
    private static final String SUCCESS_CODE = "OK";

    @Autowired
    private SmsProperties smsProperties;

    // 阿里云客户端（首次使用时创建）
    private volatile Client client;

    // 发送短信验证码
    @Override
    public boolean sendCode(String phone, String code, int expireMin) {
        if (!StringUtils.hasText(phone)) {
            log.warn("发送短信失败: 手机号为空");
            return false;
        }
        // 未配置密钥时直接失败，避免用户收不到验证码却提示发送成功（本地调试请使用 is-confirm=false 模拟模式）
        if (!StringUtils.hasText(smsProperties.getAccessKeyId()) || !StringUtils.hasText(smsProperties.getAccessKeySecret())) {
            log.error("阿里云短信密钥未配置，无法发送短信, 手机号: {}", maskPhone(phone));
            return false;
        }

        JSONObject params = new JSONObject();
        params.put("code", code);
        // 模板中的有效期（分钟）占位参数
        params.put("min", String.valueOf(expireMin));
        String templateCode = smsProperties.getTemplateCode();
        try {
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSignName(smsProperties.getSignName())
                    .setTemplateCode(templateCode)
                    .setPhoneNumber(phone)
                    .setTemplateParam(params.toJSONString());
            SendSmsVerifyCodeResponse response = getClient().sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            if (response.getBody() != null && SUCCESS_CODE.equalsIgnoreCase(response.getBody().getCode())) {
                log.info("短信发送成功, 手机号: {}, templateCode: {}", maskPhone(phone), templateCode);
                return true;
            }
            String errorMsg = response.getBody() != null ? response.getBody().getMessage() : "响应体为空";
            log.error("短信发送失败, 手机号: {}, templateCode: {}, 原因: {}", maskPhone(phone), templateCode, errorMsg);
            return false;
        } catch (TeaException e) {
            log.error("调用阿里云短信接口异常, 错误码: {}, 详情: {}", e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("调用短信发送服务发生非预期异常", e);
            return false;
        }
    }

    // 双重检查锁定创建阿里云客户端
    private Client getClient() throws Exception {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    Config config = new Config()
                            .setAccessKeyId(smsProperties.getAccessKeyId())
                            .setAccessKeySecret(smsProperties.getAccessKeySecret());
                    config.endpoint = smsProperties.getEndpoint();
                    this.client = new Client(config);
                }
            }
        }
        return client;
    }

    // 手机号脱敏（保留前3后4）
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return "****";
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }
}
